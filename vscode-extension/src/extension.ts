import * as vscode from 'vscode';
import { BreakPanel } from './breakPanel';
import { breakSeconds, readConfig } from './config';

let scheduler: NodeJS.Timeout | undefined;
let noticeTimer: NodeJS.Timeout | undefined;
let panel: BreakPanel | undefined;
let counter = 0;
let paused = false;
let lastActivity = Date.now();
let status: vscode.StatusBarItem;

export function activate(ctx: vscode.ExtensionContext): void {
  status = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 0);
  status.command = 'takeBreak.toggle';
  ctx.subscriptions.push(status);

  const touch = () => (lastActivity = Date.now());
  ctx.subscriptions.push(
    vscode.workspace.onDidChangeTextDocument(touch),
    vscode.window.onDidChangeTextEditorSelection(touch),
    vscode.window.onDidChangeWindowState((s) => s.focused && touch()),
    vscode.workspace.onDidChangeConfiguration((e) => e.affectsConfiguration('takeBreak') && reschedule()),
    vscode.commands.registerCommand('takeBreak.skip', () => panel?.dispose()),
    vscode.commands.registerCommand('takeBreak.now', () => startBreak(true)),
    vscode.commands.registerCommand('takeBreak.toggle', () => {
      paused = !paused;
      reschedule();
    }),
    { dispose: stop },
  );
  reschedule();
}

export function deactivate(): void {
  stop();
}

function stop(): void {
  clearInterval(scheduler);
  clearTimeout(noticeTimer);
  scheduler = noticeTimer = undefined;
}

function reschedule(): void {
  stop();
  status.text = paused ? '$(debug-pause) Breaks paused' : '$(coffee) Breaks on';
  status.show();
  if (paused) {
    return;
  }
  const cfg = readConfig();
  scheduler = setInterval(() => {
    startBreak(false);
    scheduleNotice();
  }, cfg.interval * 1000);
  scheduleNotice();
}

function scheduleNotice(): void {
  clearTimeout(noticeTimer);
  const { interval, notice } = readConfig();
  if (notice > 0) {
    noticeTimer = setTimeout(
      () => vscode.window.setStatusBarMessage('Your break starts soon', notice * 1000),
      (interval - notice) * 1000,
    );
  }
}

function startBreak(force: boolean): void {
  const cfg = readConfig();
  if (panel) {
    return; // drop the tick while a break is already showing
  }
  if (!force && (Date.now() - lastActivity) / 1000 >= cfg.idleThreshold) {
    return; // user is already away
  }
  counter += 1;
  panel = new BreakPanel(breakSeconds(cfg, counter), () => (panel = undefined));
}
