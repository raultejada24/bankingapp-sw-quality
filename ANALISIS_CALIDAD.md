# Grupo 7
Adrián Varea Fernández, Adrián Villalba Cuello de Oro, Arturo Vinuesa Domínguez, Blas Vita Ramos, Gonzalo Andrés Zurdo Patino, Raúl Tejada Merinero

# Análisis de Calidad del Código

## Índice
1. [Introducción y objetivo](#1-introducción-y-objetivo)
2. [Captura de Pantalla del Overview de SonarQube](#2-captura-de-pantalla-del-overview-de-sonarqube)
3. [Resultados del análisis automático y manual](#3-resultados-del-análisis-automático-y-manual)
4. [Conclusiones](#4-conclusiones)
5. [Anexos y capturas](#5-anexos-y-capturas)

---

## 1. Introducción y objetivo

En el presente documento se detalla el análisis de calidad de software realizado sobre el repositorio del proyecto `banking-app-2026`. Nuestro objetivo principal ha sido identificar, clasificar y documentar los "bad smells" (malos olores) presentes en el código base, prestando especial atención a la clase `AccountService.java`. 

Para garantizar una revisión exhaustiva, el equipo ha aplicado un enfoque híbrido. Por un lado, hemos ejecutado un escaneo automatizado mediante la plataforma SonarCloud, lo que nos ha proporcionado una visión global de las métricas de mantenibilidad, fiabilidad y seguridad del sistema. Por otro lado, hemos llevado a cabo una inspección manual minuciosa, indispensable para detectar problemas de diseño o violaciones de principios arquitectónicos (como SOLID, DRY o la Ley de Demeter) que las herramientas automáticas suelen pasar por alto. 

Siguiendo estrictamente las directrices de la práctica, este informe se centra de manera exclusiva en la fase de detección, análisis y diagnóstico de los problemas, dejando la refactorización y la implementación de pruebas para etapas posteriores del proyecto.

---

## 2. Captura de Pantalla del Overview de SonarQube

![Overview SonarQube](img/capturas/OverView.png)
![Overview SonarQube](img/capturas/OverView2.png)

En las capturas superiores se muestra el estado general del proyecto tras el primer escaneo. Se pueden observar las métricas de mantenibilidad (Code Smells), fiabilidad y seguridad antes de aplicar cualquier corrección.

---

## 3. Resultados del análisis automático y manual

### Issue 1: Duplicación del literal "Deposit Confirmation"
**Reporte de la issue**:

![Issue 1](img/capturas/Issue1_1.png)
![Issue 1](img/capturas/Issue1_2.png)
![Issue 1](img/capturas/Issue1_3.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 107, 114, 156, 163.
- Tipo: Code Smell (Critical).
- Descripción: El literal "Deposit Confirmation" se repite cuatro veces en el código para definir el asunto de las notificaciones, tanto para el canal de Email como para el de SMS.
- Justificación: Es un problema real de mantenibilidad. Al tener el mismo texto "hardcodeado" en varios puntos, cualquier cambio futuro en el mensaje obligaría a modificar el código en muchos lugares, aumentando el riesgo de olvidar alguno y generar algún tipo de inconsistencia. Lo adecuado sería extraer este valor a una constante única para poder centralizar el mensaje y facilitar su gestión.

#### Refactorización realizada

```java
// 1. Declaración de la constante al inicio de la clase AccountService
private static final String DEPOSIT_CONFIRMATION_SUBJECT = "Deposit Confirmation";
// 2. Sustitución en los métodos deposit() (Ejemplo en canal EMAIL)
emailService.sendNotification(
        account.getUser(),
        Notification.NotificationType.DEPOSIT,
        DEPOSIT_CONFIRMATION_SUBJECT, // <-- Uso de la constante
        String.format("Deposit of %.2f EUR. New balance: %.2f EUR",
                amount, account.getBalance()));
```

Explicación de la solución: Se ha extraído el literal de texto a una constante privada `DEPOSIT_CONFIRMATION_SUBJECT` a nivel de clase. Con esto eliminamos la duplicación (violación del principio DRY) y centralizamos el mensaje. Si en el futuro el banco decide cambiar el asunto del correo, solo habrá que modificar el valor de esta constante en una única línea, evitando posibles olvidos o inconsistencias en los métodos de notificación.

---

### Issue 2: Variable "seccondAccount" sin uso en AccountService
**Reporte de la issue**:

![Issue 2](img/capturas/Issue2_1.png)
![Issue 2](img/capturas/Issue2_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 185.
- Tipo: Code Smell (Minor).
- Descripción: Se ha dejado declarada una variable llamada `seccondAccount` que no hace nada en el método de retiro.
- Justificación: Es un problema real aunque de baja prioridad. Es simplemente código muerto que sobra. Al leer el código, da la sensación de que falta algo por programar o que se ha quedado ahí después de un borrador previo, por lo que debería eliminarse para no confundir.

#### Refactorización realizada

```java
// Se muestra exactamente el mismo código, pero eliminando la variable adicional seccondAccount
@Transactional
public Acount withdraw(String accountNumber, double amount, String description) {
        if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
        }

        if (amount > 5000) {
                throw new IllegalArgumentException("Amount exceeds maximum withdrawal limit");
        }

        Account account = getAccount(accountNumber);
        // Se ha eliminado la variable adicional que no se utilizaba
}
```

Explicación de la solución: Se elimina la variable `seccondAccount` del método de retiro, ya que no se utiliza en ninguna parte de la lógica. Esto permite limpiar el código, mejorando su legibilidad y evitando confusiones futuras sobre funcionalidades inexistentes o incompletas.

---

### Issue 3: Las cadenas de texto no se comparan usando "equals()"
**Reporte de la issue**:

![Issue 3](img/capturas/Issue3_1.png)
![Issue 3](img/capturas/Issue3_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 235.
- Tipo: Bug (Major).
- Descripción: Se está utilizando el operador de igualdad referencial `==` para comparar dos números de cuenta que son de tipo String.
- Justificación: Es un problema real y grave. El operador `==` comprueba si ambos objetos son la misma instancia en memoria, no si tienen el mismo contenido, por lo que en este caso la comparación podría devolver false aunque los números sean idénticos. Lo que habría que hacer es cambiar esta línea por `m.getAccountNumber().equals(o.getAccountNumber())`.

#### Refactorización realizada

```java
// Se cambia la línea de código identificada con el mal olor, de manera que pase de usar == a usar equals()

if (m.getAccountNumber().equals(o.getAccountNumber())) {
```

Explicación de la solución: Se modifica la línea `if (m.getAccountNumber() == o.getAccountNumber()) {` que se muestra en la imagen por la línea `if (m.getAccountNumber().equals(o.getAccountNumber())) {` para que el programa pueda realizar correctamente la comparación que se requiere como condición en el if.

---

### Issue 4: Nombres de variables no descriptivos en AccountService
**Reporte de la issue**:

![Issue 4](img/capturas/Issue4.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 231 y 232.
- Tipo: Mantenibilidad (Nombres crípticos).
- Descripción: En el método de transferencia se usan las letras `m` y `o` para referirse a las cuentas de origen y destino.
- Justificación: Es un problema real. El uso de variables de una sola letra obliga a cualquier programador que lea el código a tener que adivinar qué cuenta es cuál. Lo correcto sería usar nombres como `sourceAccount` y `destinationAccount` para que el código se explique por sí solo sin necesidad de comentarios.

#### Refactorización realizada

```java
// Se sustituye m y o como nombres de las variables que representan las cuentas de origen y de destino por los
//nombres sourceAccount para la variable de la cuenta de origen y destinationAccount para la variable de la
//cuenta de destino

@Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > 20000) {
            throw new IllegalArgumentException("Amount exceeds maximum transfer limit");
        }

        //m y o ahora son sourceAccount y destinationAccount
        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);

        // Validate same account
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        // Check balance
        if (sourceAccount.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Perform transfer
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        // Record transactions
        Transaction sentTransaction = new Transaction(sourceAccount,
                Transaction.TransactionType.TRANSFER_SENT,
                amount,
                "Transfer to " + toAccountNumber);
        sentTransaction.setDestinationAccountNumber(toAccountNumber);
        transactionRepository.save(sentTransaction);

        Transaction receivedTransaction = new Transaction(destinationAccount,
                Transaction.TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer from " + fromAccountNumber);
        receivedTransaction.setDestinationAccountNumber(fromAccountNumber);
        transactionRepository.save(receivedTransaction);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        User.NotificationType notifType = sourceAccount.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        }

        User.NotificationType notifTypeTo = destinationAccount.getUser().getNotificationType();
        if (notifTypeTo == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    destinationAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Received",
                    String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR",
                        amount, fromAccountNumber, destinationAccount.getBalance()));
        } else if (notifTypeTo == User.NotificationType.SMS) {
            smsService.sendNotification(
                destinationAccount.getUser(),
                Notification.NotificationType.TRANSFER,
                "Transfer Received",
                String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR", amount, fromAccountNumber, destinationAccount.getBalance()));
        }
    }
```
Explicación de la solución: Se sustituyen los nombres de las variables correspondientes a la cuenta de origen y de destino (`m` y `o` respectivamente) por nombres más descriptivos que permitan identificar y localizar fácilmente cada una de estas variables y que cada una defina de manera clara lo que representa (en este caso, `sourceAccount` para la cuenta de origen y `destinationAccount`para la cuenta destino).

---

### Issue 5: Validaciones de negocio redundantes e inalcanzables
**Reporte de la issue**:

![Issue 5](img/capturas/Issue5.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 87 a 89.
- Tipo: Lógica redundante (Dead Code).
- Descripción: Se comprueba si el importe es mayor de 10.000 y, justo después, si es mayor de 50.000 para lanzar el mismo error.
- Justificación: Es un problema de lógica real. Si alguien intenta ingresar 60.000, el programa saltará en el primer "if" (el de 10.000) y nunca llegará a evaluar el segundo. Esto hace que el código sea confuso y parezca que los límites de seguridad no están bien definidos o que se ha copiado y pegado el código sin revisarlo.

#### Refactorización realizada

```java
        if (amount == 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > 10000) {
            throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
        }

        // Se ha eliminado la condición que comprueba si el importe es mayor de 50000

```
Explicación de la solución: Se ha eliminado la validación `amount > 50000` por ser lógicamente inalcanzable. Dado que existe una restricción previa más estricta `(amount > 10000)`, cualquier valor que supere los 50000 será capturado primero por el límite de 10000, lanzando la excepción y deteniendo la ejecución. Esta redundancia generaba 'código muerto' que dificultaba el mantenimiento, por lo se ha tenido que quitar.

---

### Issue 6: Duplicación de lógica en los métodos de depósito
**Reporte de la issue**:

![Issue 6](img/capturas/Issue6.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos deposit (líneas 76 a 169).
- Tipo: Violación del principio DRY (Don't Repeat Yourself).
- Descripción: Existen dos métodos para depositar dinero que repiten exactamente las mismas validaciones y la misma lógica de guardado y notificación.
- Justificación: Es un problema real de duplicación. Si en el futuro el banco decide cambiar una regla de depósito, el desarrollador tendrá que modificar dos métodos distintos. El método corto (sin descripción) debería simplemente llamar al método largo pasando una descripción por defecto, evitando así tener el código duplicado.

#### Refactorización realizada

```java
// Versión refactorizada: método sobrecargado que reutiliza lógica
@Transactional
public Account deposit(String accountNumber, double amount) {
    // Llamar al método completo con descripción por defecto
    return deposit(accountNumber, amount, "Deposit");
}

@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_DEPOSIT_LIMIT) {
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
    }

    Account account = getAccount(accountNumber);
    account.deposit(amount);
    
    Transaction transaction = new Transaction(account, 
            Transaction.TransactionType.DEPOSIT, 
            amount, 
            description);
    transactionRepository.save(transaction);
    accountRepository.save(account);

    // Enviar notificación (lógica centralizada)
    sendNotification(account, Notification.NotificationType.DEPOSIT, 
            DEPOSIT_CONFIRMATION_SUBJECT,
            String.format("Deposit of %.2f EUR. New balance: %.2f EUR", 
                    amount, account.getBalance()));

    return account;
}
```

Explicación de la solución: Se refactoriza utilizando sobrecarga de métodos. El método `deposit(String, double)` ahora simplemente delega al método `deposit(String, double, String)` con una descripción por defecto ("Deposit"). Esto elimina completamente la duplicación de validaciones y lógica, centralizando la funcionalidad en un único punto. Si en el futuro cambian las reglas de depósito, solo hay una clase de la que cambiar el código. Además, se aprovecha para invocar un método privado `sendNotification()` que centraliza la lógica de envío, evitando más duplicación.

---

### Issue 7: Nomenclatura inadecuada en métodos de borrado
**Reporte de la issue**:

![Issue 7](img/capturas/Issue7.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 301.
- Tipo: Diseño de API / Mantenibilidad.
- Descripción: El método para eliminar una cuenta se llama simplemente "rm".
- Justificación: Es un mal olor claro. Aunque "rm" es un comando conocido en sistemas Linux, en el contexto de un servicio Java de una aplicación bancaria se deben usar nombres verbales completos como "deleteAccount". Las abreviaturas crípticas reducen la legibilidad de la arquitectura del sistema.

#### Refactorización realizada

```java
// ANTES (método con nombre críptico):
public void rm(String accountNumber) {
    Account account = getAccount(accountNumber);
    accountRepository.delete(account);
}

// DESPUÉS (método con nombre descriptivo):
public void deleteAccount(String accountNumber) {
    Account account = getAccount(accountNumber);
    accountRepository.delete(account);
}

// Alternativa incluso más descriptiva si se requieren validaciones:
@Transactional
public void deleteAccount(String accountNumber) {
    Account account = getAccount(accountNumber);
    
    // Validación: no eliminar si hay saldo pendiente
    if (account.getBalance() > 0) {
        throw new IllegalStateException(
            "Cannot delete account with remaining balance: " + account.getBalance());
    }
    
    accountRepository.delete(account);
}
```

Explicación de la solución: Se renombra el método `rm()` a `deleteAccount()`, aplicando una nomenclatura clara y descriptiva que deja evidente la intención del método. Esto mejora la legibilidad de toda la aplicación, facilita el descubrimiento de métodos mediante autocompletar de IDEs, y alinea el código con convenciones estándar de Java (nombres verbales en inglés, sin abreviaturas). Además, el nombre descriptivo permite añadir validaciones de negocio más específicas sin que el nombre quede obsoleto, como validar que no exista saldo pendiente antes de eliminar la cuenta.

---

### Issue 8: Números mágicos/hardcodeados
**Reporte de la issue**:

![Issue 8_1](img/capturas/Issue8_1.png)
![Issue 8_2](img/capturas/Issue8_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Líneas 67-78 y 114-125 (en `deposit`), línea 179 (en `withdraw`) y línea 226 (en `transfer`).
- Tipo: Diseño de API / Mantenibilidad (Code Smell).
- Descripción: Se utilizan números "mágicos" (hardcodeados) directamente en las condiciones lógicas para definir los límites de negocio del banco: 10.000 y 50.000 para depósitos, 5.000 para retiros y 20.000 para transferencias.
- Justificación: Es un mal olor real. Las reglas de negocio cambian con el tiempo y tenerlas sueltas como números crudos por todo el código hace que el mantenimiento sea complicado y propenso a errores. Si el banco cambia un límite, hay que buscar y modificar el número exacto línea por línea. Lo correcto sería definir estos valores como constantes (por ejemplo, `MAX_WITHDRAWAL_LIMIT = 5000`) al inicio de la clase.

#### Refactorización realizada

```java
// Declaración de constantes al inicio de la clase AccountService
private static final double MAX_DEPOSIT_LIMIT = 10000.0;
private static final double MAX_WITHDRAWAL_LIMIT = 5000.0;
private static final double MAX_TRANSFER_LIMIT = 20000.0;

// Uso en los métodos (ejemplo en deposit):
@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_DEPOSIT_LIMIT) {  // Uso de constante en lugar de número mágico
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
    }
    // ... resto del código
}

// En withdraw:
@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_WITHDRAWAL_LIMIT) {  // Uso de constante
        throw new IllegalArgumentException("Amount exceeds maximum withdrawal limit");
    }
    // ... resto del código
}

// En transfer:
@Transactional
public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_TRANSFER_LIMIT) {  // Uso de constante
        throw new IllegalArgumentException("Amount exceeds maximum transfer limit");
    }
    // ... resto del código
}
```

Explicación de la solución: Se extraen todos los números "mágicos" (10000, 5000, 20000) y se convierten en constantes significativas: `MAX_DEPOSIT_LIMIT`, `MAX_WITHDRAWAL_LIMIT` y `MAX_TRANSFER_LIMIT`. Esto tiene múltiples ventajas: (1) mejora la legibilidad del código, ya que el nombre de la constante explica qué representa el número, (2) facilita el mantenimiento porque si el banco decide cambiar los límites, solo hay que modificar una línea al inicio de la clase, (3) reduce el riesgo de errores por búsqueda y reemplazo defectuosa, y (4) permite que los límites sean fácilmente accesibles desde otros métodos o clases si fuera necesario.

---

### Issue 9: Condicionales redundantes y separadas
**Reporte de la issue**:

![Issue 9_1](img/capturas/Issue9_1.png)
![Issue 9_2](img/capturas/Issue9_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 77-89 y 126-138 (en ambos métodos `deposit`).
- Tipo: Lógica redundante / Code Smell (Inspección manual).
- Descripción: Se están utilizando dos bloques `if` separados para comprobar si el importe es igual a cero (`amount == 0`) y, a continuación, otro para comprobar si es menor que cero (`amount < 0`), lanzando exactamente la misma excepción en ambos casos.
- Justificación: Es un problema real de calidad de código. Resulta innecesariamente verboso y repite la misma lógica de error. Esto se puede simplificar fácilmente en una única condición `if (amount <= 0)`, mejorando la legibilidad.

#### Refactorización realizada

```java
// ANTES (código redundante con dos validaciones separadas):
@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount == 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount < 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_DEPOSIT_LIMIT) {
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
    }
    // ... resto del código
}

// DESPUÉS (código refactorizado, validación única y clara):
@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount <= 0) {  // Una sola condición que cubre ambos casos
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_DEPOSIT_LIMIT) {
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
    }
    // ... resto del código
}
```

Explicación de la solución: Se fusionan las dos validaciones separadas en una sola: `if (amount <= 0)`. Esto es más eficiente, más legible y mantiene exactamente la misma semántica. El operador `<=` cubre tanto el caso de cero como el de negativos en una única expresión, reduciendo la complejidad ciclomática del método y haciendo el código más fácil de mantener. Además, los compiladores/intérpretes moderna pueden optimizar mejor este tipo de validaciones simples.

---

### Issue 10: Duplicación masiva de la lógica de notificaciones
**Reporte de la issue**:

![Issue 10_1](img/capturas/Issue10_1.png)
![Issue 10_2](img/capturas/Issue10_2.png)
![Issue 10_3](img/capturas/Issue10_3.png)
![Issue 10_4](img/capturas/Issue10_4.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. El bloque se repite 5 veces (líneas 102-118, 151-166, 201-213, 266-278 y 281-294).
- Tipo: Violación del principio DRY (Don't Repeat Yourself) / Code Smell (Inspección manual).
- Descripción: La lógica para comprobar qué tipo de notificación tiene configurada el usuario (`EMAIL` o `SMS`) y realizar el envío a través del servicio correspondiente está copiada y pegada a lo largo de todos los métodos de operaciones bancarias.
- Justificación: Es un problema real y grave de mantenibilidad. Si en el futuro se añade un nuevo canal de notificación, habrá que modificar el código en 5 lugares distintos, aumentando el riesgo de errores. La solución ideal sería extraer esta lógica a un método privado genérico.

#### Refactorización realizada

```java
// Método privado que centraliza toda la lógica de notificaciones
private void sendNotification(Account account, Notification.NotificationType type, 
                              String subject, String message) {
    User user = account.getUser();
    User.NotificationType notifType = user.getNotificationType();
    
    switch (notifType) {
        case EMAIL:
            emailService.sendNotification(user, type, subject, message);
            break;
        case SMS:
            smsService.sendNotification(user, type, subject, message);
            break;
        default:
            throw new UnsupportedOperationException(
                "Unsupported notification type: " + notifType);
    }
}

// Ahora todos los métodos usan la versión centralizada:
@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount > MAX_DEPOSIT_LIMIT) {
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
    }

    Account account = getAccount(accountNumber);
    account.deposit(amount);
    
    Transaction transaction = new Transaction(account, 
            Transaction.TransactionType.DEPOSIT, 
            amount, 
            description);
    transactionRepository.save(transaction);
    accountRepository.save(account);

    // Una única llamada en lugar de 8-10 líneas de código duplicado
    sendNotification(account, Notification.NotificationType.DEPOSIT, 
            DEPOSIT_CONFIRMATION_SUBJECT,
            String.format("Deposit of %.2f EUR. New balance: %.2f EUR", 
                    amount, account.getBalance()));

    return account;
}

@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    // ... validaciones ...
    
    // Una única llamada centralizada
    sendNotification(account, Notification.NotificationType.WITHDRAWAL, 
            WITHDRAWAL_CONFIRMATION_SUBJECT,
            String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", 
                    amount, account.getBalance()));
    
    return account;
}
```

Explicación de la solución: Se crea un método privado `sendNotification()` que encapsula toda la lógica de envío. Este método recibe los parámetros relevantes (cuenta, tipo de notificación, asunto y mensaje) y se encarga de determinar el canal preferido del usuario (`EMAIL` o `SMS`) y delegarlo al servicio correspondiente. Ventajas: (1) El código se vuelve mucho más legible, (2) Si se añade un nuevo canal en el futuro solo hay que modificar el método `sendNotification()`, (3) Reduce toda la lógica copiada de 8-10 líneas a una única llamada legible, (4) Facilita el testing, ya que se puede mockear el método de notificación de forma centralizada.
---

### Issue 11: Método con exceso de responsabilidades (Long Method)
**Reporte de la issue**:

![Issue 11](img/capturas/Issue11.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `transfer` (líneas 223 a 296).
- Tipo: Long Method / Violación de Single Responsibility Principle (Inspección manual).
- Descripción: El método `transfer` es demasiado largo y asume demasiadas responsabilidades: valida límites, comprueba saldos, realiza retiros e ingresos, crea transacciones, guarda en base de datos y envía notificaciones.
- Justificación: Es un problema real de diseño. Al acumular tantas operaciones, el método tiene una carga cognitiva muy alta, es difícil de testear y propenso a errores. Debería refactorizarse dividiéndolo en submétodos más pequeños y específicos.

#### Refactorización realizada

```java
// REFACTORIZACIÓN: Dividir el método transfer en submétodos
@Transactional
public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
    Account sourceAccount = getAccount(fromAccountNumber);
    Account destinationAccount = getAccount(toAccountNumber);
    validateTransfer(sourceAccount, destinationAccount, amount);
    performTransfer(sourceAccount, destinationAccount, fromAccountNumber, toAccountNumber, amount);
    notifyTransfer(sourceAccount, destinationAccount, toAccountNumber, fromAccountNumber, amount);
}

private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    if (amount <= 0) throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
    if (amount > MAX_TRANSFER_LIMIT) throw new IllegalArgumentException(ERROR_TRANSFER_LIMIT_EXCEEDED);
    if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) 
        throw new IllegalArgumentException(ERROR_SAME_ACCOUNT_TRANSFER);
    if (sourceAccount.getBalance() < amount) throw new IllegalArgumentException(ERROR_INSUFFICIENT_FUNDS);
}

private void performTransfer(Account sourceAccount, Account destinationAccount, 
                             String fromAccountNumber, String toAccountNumber, double amount) {
    sourceAccount.withdraw(amount);
    destinationAccount.deposit(amount);
    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);
}

private void notifyTransfer(Account sourceAccount, Account destinationAccount, 
                           String toAccountNumber, String fromAccountNumber, double amount) {
    sendNotification(sourceAccount, Notification.NotificationType.TRANSFER, TRANSFER_SENT_SUBJECT, "...");
    sendNotification(destinationAccount, Notification.NotificationType.TRANSFER, TRANSFER_RECEIVED_SUBJECT, "...");
}
```

Explicación de la solución: Se refactoriza `transfer()` extrayendo su lógica en tres submétodos: `validateTransfer()`, `performTransfer()` y `notifyTransfer()`. El método principal es ahora muy legible (5 líneas), cada submétodo tiene una responsabilidad única y es fácil de testear aisladamente.

---

### Issue 12: Literales de texto duplicados en Excepciones
**Reporte de la issue**:

![Issue 12_1](img/capturas/Issue12_1.png)
![Issue 12_2](img/capturas/Issue12_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Mensaje `"Amount must be positive"` en líneas 79, 82, 128, 131, 177 y 225. Mensaje `"Amount exceeds maximum deposit limit"` en líneas 85, 88, 134 y 137.
- Tipo: Violación del principio DRY / Code Smell (Inspección manual).
- Descripción: Los mensajes de error de las validaciones están escritos como cadenas de texto ("Strings") literales repetidas múltiples veces a lo largo de los métodos `deposit`, `withdraw` y `transfer`.
- Justificación: Es un problema real de mantenibilidad. Tener mensajes "hardcodeados" y duplicados dificulta la consistencia del sistema. Si se desea internacionalizar la aplicación o simplemente corregir una errata en el mensaje, el desarrollador debe buscar y modificar cada instancia individualmente, aumentando el riesgo de inconsistencias. Lo adecuado es centralizar estos textos en constantes de clase.

#### Refactorización realizada

```java
// Declaración de SOLO las 2 constantes de error que se usan en el código
// Nota: Los mensajes de validación (amount, limit, insufficient) están encapsulados en excepciones personalizadas (Issue 13)
private static final String ERROR_SAME_ACCOUNT_TRANSFER = "Cannot transfer to same account";
private static final String ERROR_CANNOT_DELETE_WITH_BALANCE = "Cannot delete account with non-zero balance";

// Uso en los métodos (ejemplo):
@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    if (amount <= 0) {
        throw new InvalidAmountException(amount);
    }
    if (amount > MAX_WITHDRAWAL_LIMIT) {
        throw new LimitExceededException("Withdrawal", amount, MAX_WITHDRAWAL_LIMIT);
    }
    Account account = getAccount(accountNumber);
    if (!account.hasSufficientBalance(amount)) {
        throw new InsufficientFundsException(account.getBalance(), amount);
    }
    account.withdraw(amount);
    // ... resto del código
}

private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    if (amount <= 0) {
        throw new InvalidAmountException(amount);
    }
    if (amount > MAX_TRANSFER_LIMIT) {
        throw new LimitExceededException("Transfer", amount, MAX_TRANSFER_LIMIT);
    }
    if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
        throw new InvalidOperationException(ERROR_SAME_ACCOUNT_TRANSFER);
    }
    if (!sourceAccount.hasSufficientBalance(amount)) {
        throw new InsufficientFundsException(sourceAccount.getBalance(), amount);
    }
}

private void deleteAccount(String accountNumber) {
    Account account = getAccount(accountNumber);
    if (account.getBalance() > 0) {
        throw new InvalidOperationException(ERROR_CANNOT_DELETE_WITH_BALANCE);
    }
    accountRepository.delete(account);
}

public Account getAccount(String accountNumber) {
    return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
}
```

Explicación de la solución: Se definen constantes SOLO para los mensajes que se reutilizan múltiples veces (ERROR_SAME_ACCOUNT_TRANSFER, ERROR_CANNOT_DELETE_WITH_BALANCE). El resto de mensajes de validación están encapsulados directamente en las excepciones personalizadas (Issue 13), lo que proporciona: (1) Separación de concerns - excepciones específicas llevan sus propios mensajes, (2) Type safety - el tipo de excepción comunica el error sin necesidad de parsear strings, (3) Facilita testing - se puede capturar la excepción específica, (4) Menos código boilerplate - solo se extraen los strings que TRUE se reutilizan, (5) Mejora de la legibilidad: el nombre descriptivo de la constante clarifica qué error se está lanzando.

---

### Issue 13: Uso excesivo de Excepciones Genéricas
**Reporte de la issue**:

![Issue 13](img/capturas/Issue13.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Líneas 63, 79, 82, 85, 88, 128, 131, 134, 137, 177, 181, 225, 228, 236, 241 y 305.
- Tipo: Anti-patrón de manejo de errores / Code Smell (Inspección manual).
- Descripción: Se utiliza sistemáticamente la excepción genérica `IllegalArgumentException` para reportar errores de naturaleza muy distinta: fallos de validación, cuenta no encontrada, fondos insuficientes o errores de borrado.
- Justificación: Es un problema real que afecta la testabilidad y la extensibilidad. Al lanzar siempre la misma excepción genérica, es imposible para las capas superiores (como un controlador de API) capturar fallos específicos para dar respuestas personalizadas al usuario (ej. diferenciar un error de "Límite excedido" de uno de "Cuenta no encontrada"). Se deberían emplear excepciones de negocio personalizadas.

#### Refactorización realizada

Se crean excepciones personalizadas en `es.codeurjc.service.exceptions` en lugar de usar `IllegalArgumentException` genérica:
- `InvalidAmountException`: Montos inválidos o negativos
- `LimitExceededException`: Límites de transacción excedidos
- `InsufficientFundsException`: Fondos insuficientes para la operación
- `AccountNotFoundException`: Cuentas no encontradas en repositorio
- `InvalidOperationException`: Operaciones inválidas (ej. transferir a la misma cuenta, eliminar cuenta con saldo)

Estas excepciones **encapsulan los mensajes de error específicos**, reemplazando el uso de constantes genéricas:

```java
// Uso en AccountService.java:
public Account getAccount(String accountNumber) {
    return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
}

private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    if (amount <= 0) {
        throw new InvalidAmountException(amount);  // Encapsula: "Amount must be > 0"
    }
    if (amount > MAX_TRANSFER_LIMIT) {
        throw new LimitExceededException("Transfer", amount, MAX_TRANSFER_LIMIT);  // Encapsula: "Exceeds limit"
    }
    if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
        throw new InvalidOperationException("Cannot transfer to same account");
    }
    if (!sourceAccount.hasSufficientBalance(amount)) {  // Usa Account.hasSufficientBalance() (Issue 15)
        throw new InsufficientFundsException(sourceAccount.getBalance(), amount);  // Encapsula: "Insufficient funds"
    }
}
```

Explicación de la solución: Se crean excepciones personalizadas específicas para cada tipo de error de negocio, cada una encapsulando información relevante. Ventajas: (1) Los controladores pueden capturar excepciones específicas para respuestas personalizadas, (2) Facilita logging y auditoría diferenciado, (3) Mejora seguridad sin exponer stacks genéricos, (4) Facilita testing, (5) Auto-documentación del código.

---

### Issue 14: Violación de la Ley de Demeter (Cadenas de mensajes)
**Reporte de la issue**:

![Issue 14](img/capturas/Issue14.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Líneas 102, 151, 201, 266 y 281.
- Tipo: Fuerte acoplamiento / Code Smell (Inspección manual).
- Descripción: Se observa el uso repetido de la cadena de llamadas `account.getUser().getNotificationType()` para determinar el canal de notificación.
- Justificación: Es un problema real de acoplamiento. Esta estructura, conocida como "choque de trenes", obliga a `AccountService` a conocer detalles íntimos de la relación entre `Account` y `User`. Si la forma en que un usuario gestiona sus notificaciones cambia, este servicio se verá afectado innecesariamente. Siguiendo la Ley de Demeter, el servicio solo debería hablar con sus "amigos inmediatos" (la cuenta), delegando en ella la obtención del tipo de notificación mediante un método como `account.getPreferredNotificationType()`.

#### Refactorización realizada

Se agrega un método delegado `getPreferredNotificationType()` en la clase `Account`:

```java
// En Account.java:
public User.NotificationType getPreferredNotificationType() {
    return user.getNotificationType();
}

// En AccountService.java - ANTES (violación de Ley de Demeter):
User.NotificationType notifType = account.getUser().getNotificationType();

// En AccountService.java - DESPUÉS (cumple Ley de Demeter):
User.NotificationType notifType = account.getPreferredNotificationType();

// Uso en sendNotification():
private void sendNotification(Account account, Notification.NotificationType type, String subject, String message) {
    User user = account.getUser();
    User.NotificationType notifType = account.getPreferredNotificationType();
    
    switch (notifType) {
        case EMAIL:
            emailService.sendNotification(user, type, subject, message);
            break;
        case SMS:
            smsService.sendNotification(user, type, subject, message);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported notification type: " + notifType);
    }
}
```

Explicación de la solución: Se agrega un método delegado `getPreferredNotificationType()` en `Account` que encapsula el acceso a `User.getNotificationType()`. Así `AccountService` no accede a cadenas de getters. Ventajas: (1) Menor acoplamiento - `AccountService` no conoce la estructura interna de `User`, (2) Mayor flexibilidad arquitectónica - cambios futuros en cómo se almacenan preferencias solo afectan a `Account`, (3) Mejor legibilidad - el nombre es autodocumentado, (4) Facilita testing - es más fácil mockear un método que cadenas complejas, (5) Encapsulación - respeta "Tell, Don't Ask".

---

### Issue 15: Validación de saldo centralizada en Account
**Reporte de la issue**:

![Issue 15](img/capturas/Issue15.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `withdraw` y `transfer`.
- Tipo: Duplicación de lógica / Feature Envy (Inspección manual).
- Descripción: La validación de saldo suficiente se realiza múltiples veces en AccountService: `if (account.getBalance() < amount)`. Esta lógica se repite en `withdraw()` y `transfer()`, cuando en realidad es responsabilidad de Account validar su propio estado.
- Justificación: Es un problema real de encapsulación. El objeto Account debería ser responsable de verificar si tiene fondos suficientes. Repetir esta validación en múltiples lugares crea duplicación y viola el patrón "Tell, Don't Ask" - AccountService debería decirle a Account "valida tu saldo" en lugar de hacer la comprobación externamente.

#### Refactorización realizada

```java
// En Account.java:
public boolean hasSufficientBalance(double amount) {
    return this.balance >= amount;
}

// En AccountService.java - ANTES (violación de Feature Envy):
@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    Account account = getAccount(accountNumber);
    
    // AccountService accede a detalles internos de Account
    if (account.getBalance() < amount) {
        throw new IllegalArgumentException("Insufficient funds");
    }
    account.withdraw(amount);
    // ...
}

// En AccountService.java - DESPUÉS (delegando a Account):
@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    Account account = getAccount(accountNumber);
    
    // Account es responsable de validar su propio estado
    if (!account.hasSufficientBalance(amount)) {
        throw new InsufficientFundsException(account.getBalance(), amount);
    }
    account.withdraw(amount);
    // ...
}

// Lo mismo en transfer():
private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    // Issue 15: Use Account's hasSufficientBalance method
    if (!sourceAccount.hasSufficientBalance(amount)) {
        throw new InsufficientFundsException(sourceAccount.getBalance(), amount);
    }
}
```

Explicación de la solución: Se centraliza la lógica de validación de saldo en el método `hasSufficientBalance()` de Account, eliminando la duplicación entre el servicio y el modelo. AccountService confía en Account para validar su propio estado interno. Ventajas: (1) Un único lugar de verdad para la validación - si las reglas cambian (ej. permitir descubierto), se modifica solo en Account, (2) Respeta encapsulación - Account es responsable de su propio estado válido, (3) Elimina Feature Envy - AccountService no accede a detalles internos (`getBalance()`), (4) Reduce duplicación - la validación no está dispersa en múltiples métodos, (5) Mejora testabilidad - es más fácil testear la lógica en Account que dispersa por el servicio.

---

### Issue 16: Agrupación de datos repetidos en notificaciones (Data Clumps)
**Reporte de la issue**:

![Issue 16](img/capturas/Issue16_1.png)
![Issue 16](img/capturas/Issue16_2.png)
![Issue 16](img/capturas/Issue16_3.png)
![Issue 16](img/capturas/Issue16_4.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 102-118, 151-166, 201-213, 266-278, 281-294.
- Tipo: Data Clumps.
- Descripción: Los métodos de notificación siempre reciben los mismos parámetros: usuario, tipo, título y mensaje.
- Justificación: Es un problema porque estos datos siempre viajan juntos, lo que indica que debería existir un objeto que los encapsule. Esto mejora la claridad y reduce errores.

#### Refactorización realizada

```java
// Crear NotificationData para agrupar datos de notificación:
public class NotificationData {
    private final Notification.NotificationType type;
    private final String subject;
    private final String message;

    public NotificationData(Notification.NotificationType type, String subject, String message) {
        this.type = type;
        this.subject = subject;
        this.message = message;
    }
    public Notification.NotificationType getType() { return type; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
}

// En AccountService:
private void sendNotification(Account account, NotificationData notifData) {
    User user = account.getUser();
    User.NotificationType notifType = user.getNotificationType();
    
    switch (notifType) {
        case EMAIL:
            emailService.sendNotification(user, notifData.getType(), 
                notifData.getSubject(), notifData.getMessage());
            break;
        case SMS:
            smsService.sendNotification(user, notifData.getType(), 
                notifData.getSubject(), notifData.getMessage());
            break;
    }
}

@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    // ... lógica de depósito ...
    NotificationData notifData = new NotificationData(
        Notification.NotificationType.DEPOSIT,
        DEPOSIT_CONFIRMATION_SUBJECT,
        String.format("Deposit of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance())
    );
    sendNotification(account, notifData);
    return account;
}
```

Explicación de la solución: Se crea la clase `NotificationData` que agrupa tipo, asunto y mensaje. Esto reduce parámetros en firmas de métodos y deja clara la cohesión conceptual de estos datos. Ventajas: (1) Reduce complejidad de firmas, (2) Agrupa datos lógicamente relacionados, (3) Facilita futuras extensiones, (4) Mejora legibilidad.

---

### Issue 17: Feature Envy en validación de saldo
**Reporte de la issue**:
 
![Issue 17](img/capturas/Issue17_1.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `transfer`.
- Tipo: Feature Envy (Fuerte Acoplamiento).
- Descripción: El servicio le pide los datos internos a la cuenta (`m.getBalance() < amount`) para tomar la decisión de lanzar la excepción, y luego le ordena retirar (`m.withdraw(amount)`).
- Justificación: Tal y como se indica en la teoría de la asignatura, este método usa más datos ajenos (de la cuenta) que propios. La lógica de verificar si hay fondos pertenece exclusivamente a la clase `Account`. Al sacar esta lógica fuera de la entidad, generamos un alto acoplamiento y violamos la encapsulación (principio "Tell, Don't Ask").

#### Refactorización realizada

```java
// ANTES - Feature Envy (pedir datos y hacer lógica externamente):
@Transactional
public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
    Account sourceAccount = getAccount(fromAccountNumber);
    Account destinationAccount = getAccount(toAccountNumber);
    
    // Esto es Feature Envy: pedir a sourceAccount su balance
    if (sourceAccount.getBalance() < amount) {
        throw new IllegalArgumentException("Insufficient funds");
    }
    sourceAccount.withdraw(amount);
}

// DESPUÉS - Delegar responsabilidad a Account:
@Transactional
public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
    Account sourceAccount = getAccount(fromAccountNumber);
    Account destinationAccount = getAccount(toAccountNumber);
    
    // Account es responsable de validar su propio estado
    if (!sourceAccount.hasSufficientBalance(amount)) {
        throw new InvalidOperationException("Insufficient funds");
    }
    sourceAccount.withdraw(amount); // Método en Account contiene su lógica interna
}
```

Explicación de la solución: Se usa el método `hasSufficientBalance()` que Account ya expone, en lugar de acceder directamente a `getBalance()` y hacer la comparación en el servicio. Esto respeta el principio "Tell, Don't Ask" - le decimos a Account "verifica si tienes suficientes fondos" en lugar de "dame tu balance para que yo verifique". Ventajas: (1) Encapsulación - Account es responsible de su propio estado, (2) Menor acoplamiento, (3) Cambios en lógica de balance solo afectan a Account, (4) Código más legible y semánticamente claro.

---

### Issue 18: Fuerte acoplamiento con detalles técnicos (Violación de Clean Architecture)
**Reporte de la issue**:

![Issue 18](img/capturas/Issue18_1.png)
![Issue 18](img/capturas/Issue18_2.png)
![Issue 18](img/capturas/Issue18_3.png)


**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `deposit`, `withdraw` y `transfer`.
- Tipo: Violación de Clean Architecture / Acoplamiento a infraestructura.
- Descripción: Se mezcla la gestión de transacciones de base de datos (`@Transactional`) con el envío de notificaciones externas mediante red (`emailService.sendNotification`).
- Justificación: Siguiendo los principios de Clean Architecture, las reglas de negocio no deben depender de los detalles técnicos. Al enviar un email dentro de una transacción abierta, acoplamos el rendimiento de la BD a la del servidor de correo.

#### Refactorización realizada

```java
// La refactorización agrupa notificaciones: ver Issue 10
// El servicio centraliza mediante sendNotification(), permitiendo:
// 1. Cambiar implementaciones sin tocar lógica de negocio
// 2. Futuros: Event-driven (publicar NotificationSentEvent en lugar de enviar directamente)

// Estructura actual que desacopla:
private void sendNotification(Account account, Notification.NotificationType type, 
                              String subject, String message) {
    User user = account.getUser();
    User.NotificationType notifType = account.getPreferredNotificationType();
    
    switch (notifType) {
        case EMAIL:
            // Aquí podríamos: eventPublisher.publishEvent(new EmailNotificationEvent(...))
            // En lugar de acoplarnos directamente
            emailService.sendNotification(user, type, subject, message);
            break;
        case SMS:
            smsService.sendNotification(user, type, subject, message);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported notification type");
    }
}
```

Explicación de la solución: Se centraliza el envío de notificaciones en `sendNotification()`, que actúa como punto de abstracción. La lógica de negocio no conoce directamente EmailNotificationService ni SmsNotificationService. Esto permite: (1) Cambiar estrategias de notificación sin modificar lógica de negocio, (2) Futuros: refactorizar a patrón event-driven, (3) Mejor testabilidad, (4) Cumple Clean Architecture – negocio ↓ aplicación ↓ infraestructura.

---

### Issue 19: Consulta masiva a base de datos sin paginación (Rendimiento)
**Reporte de la issue**:

![Issue 19](img/capturas/Issue19_1.png)


**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `getTransactions`.
- Tipo: Falta de Cohesión / Rendimiento.
- Descripción: El método `transactionRepository.findByAccountOrderByTimestampDesc(account)` devuelve la lista completa de todas las transacciones de una cuenta de golpe en un objeto `List<Transaction>`.
- Justificación: En un sistema real, una cuenta bancaria acumula miles de transacciones con el tiempo. Traer todos esos registros de golpe a la memoria penaliza el rendimiento y dificulta la mantenibilidad a largo plazo, pudiendo causar caídas por falta de memoria (Out Of Memory). Debería aplicarse paginación desde el repositorio para traer los datos en lotes pequeños.

#### Refactorización realizada

```java
// ANTES - Consulta masiva sin paginación (N+1, memoria excesiva):
public List<Transaction> getAllTransactions() {
    return transactionRepository.findAll(); // Carga TODOS los registros en memoria
}

// DESPUÉS - Con paginación y lazy loading:
public Page<Transaction> getTransactionsPaginated(String accountNumber, int page, int size) {
    Account account = getAccount(accountNumber);
    // Usar Page<T> de Spring Data para automático LIMIT/OFFSET
    return transactionRepository.findByAccountOrderByTimestampDesc(account, 
            PageRequest.of(page, size));
}

// En AccountService:
@Transactional(readOnly = true)
public List<Transaction> getTransactions(String accountNumber) {
    Account account = getAccount(accountNumber);
    // Limitar a últimas 100 transacciones, nunca cargar sin límite
    return transactionRepository.findByAccountOrderByTimestampDesc(account)
            .stream()
            .limit(100)
            .collect(Collectors.toList());
}
```

Explicación de la solución: Se implementa paginación usando Spring Data's `Page<T>` y `Pageable`, o se limita explícitamente resultados. Ventajas: (1) Memory-efficient - no cargar millones de registros, (2) Performance - consultas rápidas con LIMIT, (3) UX paginada - usuarios navegan por páginas, (4) Predecible - no sorpresas de OutOfMemory. Best practice: siempre usar paginación en endpoints que retornan colecciones.

---

### Issue 20: Falta de control de flujo por defecto (Fallo silencioso)
**Reporte de la issue**:

![Issue 20](img/capturas/Issue20_1.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, bloques de notificación en todos los métodos operativos.
- Tipo: Complejidad Ciclomática / Lógica incompleta.
- Descripción: El código evalúa `if (notifType == EMAIL)` y luego usa `else if (notifType == SMS)`, pero carece de un bloque `else` final.
- Justificación: Al dejar "caminos" lógicos sin cubrir, la complejidad de las pruebas aumenta porque ese camino invisible no tiene un comportamiento definido. Si en el futuro se añade un nuevo valor al Enum `NotificationType` (por ejemplo, `PUSH`) y se olvida modificar este bloque, el sistema pasará por alto el envío de forma silenciosa sin lanzar ninguna alerta o excepción, dificultando mucho el debugging.

#### Refactorización realizada

```java
// ANTES (sin control de flujo por defecto):
if (notifType == User.NotificationType.EMAIL) {
    emailService.sendNotification(user, type, subject, message);
} else if (notifType == User.NotificationType.SMS) {
    smsService.sendNotification(user, type, subject, message);
}
// Si notifType es cualquier otro valor, no pasa nada (fallo silencioso)

// DESPUÉS (opción 1: usando if/else):
if (notifType == User.NotificationType.EMAIL) {
    emailService.sendNotification(user, type, subject, message);
} else if (notifType == User.NotificationType.SMS) {
    smsService.sendNotification(user, type, subject, message);
} else {
    // Fallo explícito para cualquier tipo no soportado
    throw new UnsupportedOperationException(
        "Unsupported notification type: " + notifType);
}

// DESPUÉS (opción 2: más limpio, usando SWITCH con default):
private void sendNotification(Account account, Notification.NotificationType type, 
                              String subject, String message) {
    User user = account.getUser();
    User.NotificationType notifType = account.getPreferredNotificationType();
    
    // Issue 20: Switch con default case - control de flujo completo
    switch (notifType) {
        case EMAIL:
            emailService.sendNotification(user, type, subject, message);
            break;
        case SMS:
            smsService.sendNotification(user, type, subject, message);
            break;
        default:
            // Fallo explícito - nunca pasa desapercibido
            throw new UnsupportedOperationException(
                "Unsupported notification type: " + notifType);
    }
}
```

Explicación de la solución: Se agrega un bloque `else` o un `default` case en el switch que lanza una excepción explícita si el tipo de notificación no es soportado. Esto convierte los fallos silenciosos en fallos ruidosos (excepciones). Ventajas: (1) Previene bugs silenciosos - cualquier tipo no manejado genera una excepción inmediata, (2) Si se añade un nuevo Enum en el futuro, el sistema falla en runtime si no se actualiza el switch, (3) Facilita debugging - la excepción aparece en logs y rastreos, (4) Mejor cobertura de tests - el default case es testeable, (5) Mejora seguridad - evita comportamientos indefinidos, (6) Clean Code - deja explícita la intención: "solo EMAIL y SMS son soportados".

---

### Issue 21: Falta de validación de existencia previa del número de cuenta
**Reporte de la issue**:

![Issue 21](img/capturas/Issue21.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `createAccount`, `generateAccountNumber`, `getAccount` (líneas 41-64).
- Tipo: Bug potencial / Violación de reglas de negocio.
- Descripción: El método genera un número de cuenta mediante `generateAccountNumber()` y lo guarda directamente sin comprobar si ya existe en la base de datos. Además, añade este número de cuenta a la cuenta y luego lo usa para buscar cuentas, lo que nos dice que es un identificador, por lo cual debe ser único.
- Justificación: Es un problema real porque no se garantiza la unicidad del número de cuenta, lo cual es un requisito crítico en un sistema bancario. Aunque la probabilidad de colisión sea muy baja, el impacto sería grave (dos cuentas con el mismo identificador). Se debería validar contra el repositorio o delegar en la base de datos con una restricción única. No se deben correr riesgos así simplemente porque sea difícil que suceda.

#### Refactorización realizada

```java
@Transactional
public Account createAccount(User user, Account.AccountType accountType) {
    String accountNumber = generateAccountNumber();
    
    // Validar que no exista una cuenta con este número
    if (accountRepository.existsByAccountNumber(accountNumber)) {
        throw new InvalidOperationException("Account number already exists: " + accountNumber);
    }
    
    Account account = new Account(accountNumber, accountType, 0);
    account.setUser(user);
    return accountRepository.save(account);
}

// En AccountRepository:
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
```

Explicación de la solución: Se agrega validación `existsByAccountNumber()` para verificar que el número de cuenta sea único antes de crear una nueva. Ventajas: (1) Garantiza unicidad de cuentas, (2) Evita corrupción de datos, (3) Claridad de intención, (4) Previene bugs silenciosos.

---

### Issue 22: Uso de tipos primitivos para representar dinero (Primitive Obsession)
**Reporte de la issue**:

![Issue 22](img/capturas/Issue22.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `deposit`, `withdraw` y `transfer` (líneas 77, 126, 175, 223).
- Tipo: Primitive Obsession.
- Descripción: Se utiliza `double` para representar cantidades de dinero (`amount`).
- Justificación: Es un problema real porque los tipos `double` pueden generar errores de precisión en operaciones financieras. Además, no encapsulan lógica de negocio como moneda o validaciones.

#### Refactorización realizada

Se crea una clase `Money` (Value Object) que encapsula cantidad y moneda:

```java
// Archivo: src/main/java/es/codeurjc/model/Money.java
public class Money implements Serializable, Comparable<Money> {
    private final double amount;
    private final String currency; // EUR, USD, etc.
    
    public Money(double amount, String currency) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        this.amount = amount;
        this.currency = currency;
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) 
            throw new IllegalArgumentException("Cannot add different currencies");
        return new Money(this.amount + other.amount, this.currency);
    }
    
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) 
            throw new IllegalArgumentException("Cannot subtract different currencies");
        return new Money(this.amount - other.amount, this.currency);
    }
    
    public boolean isAtLeast(Money other) {
        return this.amount >= other.amount;
    }
}

// Uso en Account:
public class Account {
    private Money balance; // En lugar de primitive double
    
    public void deposit(Money amount) {
        this.balance = balance.add(amount);
    }
    
    public void withdraw(Money amount) {
        if (!balance.isAtLeast(amount)) 
            throw new InsufficientFundsException();
        this.balance = balance.subtract(amount);
    }
}
```

Explicación de la solución: Se crea Value Object `Money` que agrupa cantidad, moneda y operaciones permitidas. Ventajas: (1) Type safety - no confundir EUR con USD, (2) Encapsulación - operaciones válidas solo en Money, (3) Previene errores - no puedes sumar money a string accidentalmente, (4) Auto-documentary - el código es más claro, (5) Facilita i18n - manejo de monedas, (6) Respeta el patrón Domain-Driven Design.

---

## 4. Conclusiones

La realización de esta práctica nos ha permitido comprobar de primera mano que un código que compila y funciona no es, ni mucho menos, un código limpio o mantenible a largo plazo.

Durante el proceso, hemos constatado que las herramientas de análisis estático como SonarCloud son un excelente punto de partida, ya que detectan de manera inmediata duplicidades y problemas de configuración básicos. Sin embargo, el verdadero valor del control de calidad ha residido en la auditoría manual. A través de nuestra inspección, hemos sacado a la luz carencias arquitectónicas significativas y una considerable deuda técnica acumulada, identificando métodos con exceso de responsabilidades, un fuerte acoplamiento entre clases y un abuso generalizado de excepciones genéricas y tipos primitivos.

En definitiva, este análisis evidencia la importancia de aplicar principios de buen diseño (Clean Code) desde las fases iniciales del desarrollo, ya que depender únicamente del funcionamiento empírico del programa compromete gravemente su escalabilidad y la futura introducción de nuevas funcionalidades.

---

## 5. Anexos y capturas

A continuación, se adjuntan evidencias del estado de la cobertura de código (Code Coverage) de la clase `AccountService` evaluada mediante el plugin JaCoCo durante la **Tarea 2**.

**Cobertura Inicial (Antes de las pruebas):**

![Cobertura JaCoCo Antes](img/TestCoverageAccountServiceBefore.png)

*En esta captura se aprecia que, inicialmente, la clase carecía por completo de pruebas unitarias (0% de ramas cubiertas).*

**Ejecución "mvn clean test":**

![Tests](img/testsPassed.png)

*Resultado de la ejecución automatizada de las pruebas unitarias. Se verifica que los 35 casos diseñados para aislar la lógica de AccountService finalizan con estado exitoso.*

**Cobertura Final (Tras implementar AccountServiceTest):**

![Cobertura JaCoCo Después](img/TestCoverageAccountServiceAfter.png)

*Tras el reparto y la implementación del plan de pruebas, se ha logrado alcanzar el 98% de cobertura de ramas (Branches) e instrucciones accesibles, estableciendo la red de seguridad necesaria para la refactorización.*

---

## Resumen de Refactorizaciones Implementadas

### Tabla: Ubicación de Cada Issue

| # | Nombre Issue | Ubicación | Líneas | Estado |
|---|---|---|---|---|
| 1 | Literales duplicados | AccountService.java | 34-38 | Implementado |
| 2 | Variable sin uso | AccountService.java (orig) | - | Documentado |
| 3 | Comparación strings | AccountService.java | 285 | Implementado |
| 4 | Nombres vars descriptivos | AccountService.java | 201-203 | Implementado |
| 5 | Condición inalcanzable | Eliminada (Issue 13) | - | Implementado |
| 6 | Duplicación deposit | AccountService.java | 101-146 | Implementado |
| 7 | Nomenclatura borrado | AccountService.java | 216-228 | Implementado |
| 8 | Magic numbers | AccountService.java | 24-26 | Implementado |
| 9 | Condicionales redundantes | AccountService.java | 121 | Implementado |
| 10 | Duplicación notificaciones | AccountService.java | 254-271 | Implementado |
| 11 | Long method | AccountService.java | 190-343 | Implementado |
| 12 | Literales excepciones | AccountService.java | 40-43 | Implementado (2 constantes) |
| 13 | Excepciones genéricas | exceptions/*.java | 5 clases, 95 líneas | Creadas |
| 14 | Ley de Demeter | Account.java | 106-109 | Agregado |
| 15 | Validación saldo duplicada | Account.java (hasSufficientBalance) | 148-150 | Implementado |
| 16 | Data Clumps | Issue 10 (centralización) | - | Resuelto |
| 17 | Feature Envy | Issue 14 + 15 (delegación) | - | Resuelto |
| 18 | Clean Architecture | Issue 10 (abstracción) | - | Resuelto |
| 19 | Paginación | transactionRepository | - | Implementable |
| 20 | Default case | AccountService.java | 259-271 | Implementado |
| 21 | Validación unicidad | createAccount() | 68-82 | Implementado |
| 22 | Primitive Obsession | Money.java | 112 líneas | Creada |

### Desglose por Archivo

**AccountService.java** (294 líneas)
- 12 issues implementadas directamente  
- Código limpio, SOLID-compliant, fácil de mantener y testear

**Excepciones Personalizadas** (src/main/java/es/codeurjc/service/exceptions/)
- `InvalidAmountException.java` (18 líneas) - Para montos inválidos o negativos
- `LimitExceededException.java` (25 líneas) - Para límites de transacción excedidos
- `InsufficientFundsException.java` (23 líneas) - Para fondos insuficientes
- `AccountNotFoundException.java` (17 líneas) - Para cuentas no encontradas
- `InvalidOperationException.java` (12 líneas) - Para operaciones inválidas
- **Uso**: Importadas y usadas en AccountService.java (líneas 11-15), reemplazando IllegalArgumentException

**Clases de Dominio** (Extensiones)
- `Account.java`: +método `getPreferredNotificationType()` (3 líneas) - Issue 14 (Ley de Demeter)
- `Account.java`: +método `hasSufficientBalance(double)` (3 líneas) - Issue 15 (validación centralizada)
- `Money.java`: Nuevo Value Object (112 líneas) - Issue 22 (Primitive Obsession)

---

### Referencias y Documentación

- [CS - Tema 2.1 – Bad Smells.docx.pdf](https://github.com/user-attachments/files/26079919/CS.-.Tema.2.1.Bad.Smells.docx.pdf)
- [Herramienta SonarQube Cloud](https://www.sonarsource.com/products/sonarqube/?s_campaign=SQ-APJ-1-Japan-Brand&s_content=Languages&s_term=sonarcloud&s_category=Paid&s_source=Paid%20Search&s_origin=Google&cq_src=google_ads&cq_cmp=23600038948&cq_con=200395503824&cq_term=sonarcloud&cq_med=&cq_plac=&cq_net=g&cq_pos=&cq_plt=gp&gad_source=1&gad_campaignid=23600038948&gbraid=0AAAAAC0fKmoueJlSX5zp_yyaBo4bekNSW&gclid=Cj0KCQjwmunNBhDbARIsAOndKpk0FsvHNiaBPCESgxmWNTwthCKwfppVPAmYKkqmSOCt1ZCuoKNGCb0aAgbUEALw_wcB)

---

*(Documento preparado por Grupo 7)*
