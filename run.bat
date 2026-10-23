@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK 17 ou superior nao encontrado. Instale um JDK e adicione javac ao PATH.
  pause
  exit /b 1
)
if not exist build\classes mkdir build\classes
javac -encoding UTF-8 -d "build\classes" ^
  "src\main\java\br\com\bolsoclaro\OrcaBemApp.java" ^
  "src\main\java\br\com\bolsoclaro\BudgetStore.java" ^
  "src\main\java\br\com\bolsoclaro\CategoryStore.java" ^
  "src\main\java\br\com\bolsoclaro\CsvStore.java" ^
  "src\main\java\br\com\bolsoclaro\Transaction.java" ^
  "src\main\java\br\com\bolsoclaro\SavingsGoalStore.java" ^
  "src\main\java\br\com\bolsoclaro\TrendChart.java" ^
  "src\main\java\br\com\bolsoclaro\AppIcon.java"
if errorlevel 1 (
  echo Falha ao compilar o OrcaBem.
  if /i "%~1"=="--compile-only" exit /b 1
  pause
  exit /b 1
)
if /i "%~1"=="--compile-only" exit /b 0
java -cp "build\classes" br.com.bolsoclaro.OrcaBemApp
