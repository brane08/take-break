import * as vscode from 'vscode';

export class BreakPanel {
  private panel: vscode.WebviewPanel;
  private timer: NodeJS.Timeout;

  constructor(seconds: number, private onEnd: () => void) {
    this.panel = vscode.window.createWebviewPanel('takeBreak', 'Time for a Break', vscode.ViewColumn.Active, {
      enableScripts: false,
    });
    let remaining = seconds;
    const render = () => (this.panel.webview.html = html(remaining));
    render();
    this.timer = setInterval(() => {
      remaining -= 1;
      if (remaining <= 0) {
        this.dispose();
      } else {
        render();
      }
    }, 1000);
    this.panel.onDidDispose(() => this.finish());
  }

  dispose(): void {
    this.panel.dispose();
  }

  private finished = false;
  private finish(): void {
    if (this.finished) {
      return;
    }
    this.finished = true;
    clearInterval(this.timer);
    this.onEnd();
  }
}

function html(remaining: number): string {
  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  return `<!DOCTYPE html><html><body style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:90vh;font-family:sans-serif">
<h1>Time for a Break</h1>
<div style="display:flex;gap:24px;font-size:6em;font-weight:bold"><div>${mm}<small style="display:block;font-size:.15em;font-weight:normal">Minutes</small></div><div>${ss}<small style="display:block;font-size:.15em;font-weight:normal">Seconds</small></div></div>
<p>Close this tab or run “Take Break: Skip Break” to skip.</p></body></html>`;
}
