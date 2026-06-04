Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
code = shell.Run("cmd /c """ & scriptDir & "\run.cmd""", 0, True)
If code <> 0 Then
    MsgBox "Take Break failed to start. Run run.cmd in a terminal for details.", _
           vbExclamation, "Take Break"
End If
