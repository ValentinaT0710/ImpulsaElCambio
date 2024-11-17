@echo off
echo Ejecutando tests detallados para ForoService...

echo.
echo 1. Test básico con logs detallados
mvn test -Dtest=ForoServiceTest -X

echo.
echo 2. Test con reporte detallado
mvn clean test surefire-report:report site -Dtest=ForoServiceTest

echo.
echo 3. Test con cobertura de código
mvn clean verify -Dtest=ForoServiceTest

echo.
echo 4. Test específico con todos los detalles
mvn test -Dtest=ForoServiceTest#crearComentario_DebeCrearComentarioExitosamente -Dmaven.test.failure.ignore=true -X

echo.
echo 5. Test con reporte XML
mvn test -Dtest=ForoServiceTest -DfailIfNoTests=false -Dsurefire.reportFormat=xml

echo.
echo Tests completados. Revisa los reportes en:
echo - target/site/surefire-report.html
echo - target/site/jacoco/index.html
echo - target/surefire-reports/
pause 