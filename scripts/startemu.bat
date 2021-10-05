@echo off

set LEN=0
set ARR[0]=""
set LEN=0
setlocal ENABLEDELAYEDEXPANSION
for /f "delims=" %%i in ('emulator -list-avds') do (
	echo     !LEN! %%i
	set ARR[!LEN!]=%%i
	set /A LEN=LEN+1
)

if !LEN! NEQ 0 (
	:SymLoop
	set /p in="Select AVD: "
	if !in! GEQ 0 if !in! LSS !LEN! (
		set S=""
		for /f "delims=" %%E in ("!in!") do set "S=!ARR[%%E]!"
		echo Selected !S!
		emulator -netdelay none -netspeed full -avd !S!
	) else (
		echo "Invalid selection, please try again."
		GOTO :SymLoop
	)
)

endlocal
