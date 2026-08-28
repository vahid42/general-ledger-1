# اجرای پروژه و Swagger

## 1. ورود به Root پروژه

```powershell
cd ..\general-ledger
```

## 2. Build کل پروژه

```powershell

mvn clean install

or 

mvn -pl ledger-bootstrap -am clean install
```

## 3. اجرای Application

```powershell
mvn -pl ledger-bootstrap spring-boot:run
```

پس از مشاهده پیام زیر:

```text
Started GeneralLedgerApplication
```

برنامه با موفقیت اجرا شده است.

## 4. دسترسی به Swagger UI

http://localhost:8080/swagger-ui/index.html

## 5. دسترسی مستقیم به OpenAPI

http://localhost:8080/v3/api-docs



#debug 
launch.json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "GeneralLedgerApplication",
            "request": "launch",
            "mainClass": "com.ledger.bootstrap.GeneralLedgerApplication",
            "projectName": "ledger-bootstrap"
        },
        {
            "type": "java",
            "name": "Attach General Ledger",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}

mvn -pl ledger-bootstrap spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005"

seelct run and debug => Attach General Ledger