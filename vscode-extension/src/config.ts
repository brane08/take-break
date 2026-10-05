import * as vscode from 'vscode';

export interface BreakConfig {
  shortBreak: number;
  longBreak: number;
  interval: number;
  notice: number;
  idleThreshold: number;
}

export function readConfig(): BreakConfig {
  const c = vscode.workspace.getConfiguration('takeBreak');
  const longBreak = c.get<number>('longBreakSeconds', 300);
  const shortBreak = c.get<number>('shortBreakSeconds', 60);
  // Mirror BreakConfig.normalized(): interval outlasts the longest break, notice precedes the break.
  const interval = Math.max(c.get<number>('intervalSeconds', 1080), Math.max(longBreak, shortBreak) + 1);
  const notice = Math.min(c.get<number>('noticeSeconds', 30), interval - 1);
  return { shortBreak, longBreak, interval, notice, idleThreshold: c.get<number>('idleThresholdSeconds', 300) };
}

// Every third break is a long one (same cadence as the desktop app).
export function breakSeconds(cfg: BreakConfig, instance: number): number {
  return instance % 3 === 0 ? cfg.longBreak : cfg.shortBreak;
}
