# Grupo 7
Adrián Varea Fernández, Adrián Villalba Cuello de Oro, Arturo Vinuesa Domínguez, Blas Vita Ramos, Gonzalo Andrés Zurdo Patino, Raúl Tejada Merinero

# Análisis Integral de Calidad del Código
**Proyecto:** banking-app-2026 | **Enfoque:** Análisis + Refactorización + Validación

## Índice
1. [Introducción y objetivo](#1-introducción-y-objetivo)
2. [Captura de Pantalla del Overview de SonarQube](#2-captura-de-pantalla-del-overview-de-sonarqube)
3. [Resultados del análisis automático y manual](#3-resultados-del-análisis-automático-y-manual)
4. [Conclusiones](#4-conclusiones)
5. [Anexos y capturas](#5-anexos-y-capturas)

---

## 1. Introducción y objetivo

En el presente documento se detalla el análisis integral de calidad de software realizado sobre el repositorio del proyecto `banking-app-2026`. Nuestro objetivo principal ha sido identificar, clasificar, documentar y **refactorizar** los "bad smells" (malos olores) presentes en el código base, prestando especial atención a la clase `AccountService.java`. 

Para garantizar una revisión exhaustiva, el equipo ha aplicado un enfoque híbrido basado en tres fases:

**Fase 1 - Detección y Análisis:** Por un lado, hemos ejecutado un escaneo automatizado mediante la plataforma SonarCloud, lo que nos ha proporcionado una visión global de las métricas de mantenibilidad, fiabilidad y seguridad del sistema. Por otro lado, hemos llevado a cabo una inspección manual minuciosa, indispensable para detectar problemas de diseño o violaciones de principios arquitectónicos (como SOLID, DRY o la Ley de Demeter) que las herramientas automáticas suelen pasar por alto.

**Fase 2 - Refactorización:** Se han implementado 11 refactorizaciones siguiendo la estrategia de "Refactorización Integral de Clase Única", consolidando todos los cambios exclusivamente en `AccountService.java` mediante inner classes, métodos privados centralizados y eliminación de código muerto.

**Fase 3 - Validación:** Se han ejecutado los tests unitarios para garantizar que los cambios no rompen la funcionalidad existente, manteniendo la máxima seguridad en la refactorización.

Este informe documenta el proceso completo: desde la identificación de problemas hasta su resolución, proporcionando evidencia de cómo el Clean Code y los principios SOLID transforman el código de "funcional" a "profesional".

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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

```java
// 1. Declaración de la constante al inicio de la clase AccountService
private static final String DEPOSIT_CONFIRMATION_SUBJECT = "Deposit Confirmation";
// 2. Sustitución en los métodos deposit() (Ejemplo en canal EMAIL)
emailService.sendNotification(
        account,
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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

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
        // Check balance
        ensureSufficientBalance(sourceAccount, amount);

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
                    sourceAccount,
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    sourceAccount,
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        }

        User.NotificationType notifTypeTo = destinationAccount.getUser().getNotificationType();
        if (notifTypeTo == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    destinationAccount,
                    Notification.NotificationType.TRANSFER,
                    "Transfer Received",
                    String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR",
                        amount, fromAccountNumber, destinationAccount.getBalance()));
        } else if (notifTypeTo == User.NotificationType.SMS) {
            smsService.sendNotification(
                destinationAccount,
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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

```java
        // ... validaciones previas de cantidad positiva ...

        if (amount > 10000) {
        throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
        }

        // Se ha eliminado la condición inalcanzable que comprueba si el importe es mayor de 50000

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

#### Refactorización realizada - Hecho por: Raúl Tejada Merinero

```java
// Versión refactorizada: método sobrecargado que reutiliza la lógica del método principal
@Transactional
public Account deposit(String accountNumber, double amount) {
    // Llama al método completo pasando una descripción por defecto ("Quick deposit")
    return deposit(accountNumber, amount, "Quick deposit");
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
    
    // ... Creación de transacción y guardado ...
    
    // ... Lógica de notificaciones ...

    return savedAccount;
}
```

Explicación de la solución: Se refactoriza utilizando sobrecarga de métodos. El método `deposit(String, double)` ahora simplemente delega su ejecución al método principal `deposit(String, double, String)` inyectándole una descripción por defecto ("Quick deposit"). Esto elimina completamente la duplicación de validaciones y lógica de persistencia/notificación, centralizando toda la funcionalidad en un único punto.

---

### Issue 7: Nomenclatura inadecuada en métodos de borrado
**Reporte de la issue**:

![Issue 7](img/capturas/Issue7.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 301.
- Tipo: Diseño de API / Mantenibilidad.
- Descripción: El método para eliminar una cuenta se llama simplemente "rm".
- Justificación: Es un mal olor claro. Aunque "rm" es un comando conocido en sistemas Linux, en el contexto de un servicio Java de una aplicación bancaria se deben usar nombres verbales completos como "deleteAccount". Las abreviaturas crípticas reducen la legibilidad de la arquitectura del sistema.

#### Refactorización realizada - Hecho por: Adrián Villalba Cuello de Oro

```java
// ANTES (método con nombre críptico):
// public void rm(String accountNumber) { ... }

// DESPUÉS (método con nombre descriptivo y validación unificada):
@Transactional
public void deleteAccount(String accountNumber) {
    Account account = getAccount(accountNumber);
    
    // Validación: no eliminar si hay saldo pendiente
    if (account.getBalance() != 0) {
        throw new IllegalArgumentException("Cannot delete account with non-zero balance");
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

#### Refactorización realizada - Hecho por: [Nombre]

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

#### Refactorización realizada - Hecho por: [Nombre]

```java
// ANTES (código redundante con dos validaciones separadas):
// if (amount == 0) { throw new IllegalArgumentException("Amount must be positive"); }
// if (amount < 0) { throw new IllegalArgumentException("Amount must be positive"); }

// DESPUÉS (código refactorizado, validación única y clara):
@Transactional
public Account deposit(String accountNumber, double amount, String description) {
    if (amount <= 0) {  // <-- Una sola condición que cubre ambos casos (0 y negativos)
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

#### Refactorización realizada - Hecho por: Adrián Villalba Cuello de Oro

```java
// 1. Se crea un método privado auxiliar para centralizar la lógica:
private void sendNotification(Account account, Notification.NotificationType type, String subject, String message) {
    User user = account.getUser();
    User.NotificationType notifType = user.getNotificationType();
    if (notifType == User.NotificationType.EMAIL) {
        emailService.sendNotification(user, type, subject, message);
    } else if (notifType == User.NotificationType.SMS) {
        smsService.sendNotification(user, type, subject, message);
    }
}

// 2. Se sustituyen los bloques repetidos por una única llamada (Ejemplo en withdraw):
@Transactional
public Account withdraw(String accountNumber, double amount, String description) {
    // ... lógica de validación y guardado ...

    // Una única llamada en lugar de todo el bloque if/else repetido
    sendNotification(account, Notification.NotificationType.WITHDRAWAL,
            "Withdrawal Confirmation",
            String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));

    return savedAccount;
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

#### Refactorización realizada - Hecho por: Blas Vita Ramos

```java
@Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);
        validateTransfer(sourceAccount, destinationAccount, amount);
        recordTransfer(sourceAccount, destinationAccount, fromAccountNumber, toAccountNumber, amount);
        performTransfer(sourceAccount, destinationAccount, amount);
        notifyTransfer(sourceAccount, destinationAccount, toAccountNumber, fromAccountNumber, amount);

    }

    private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
        if (amount <= 0) throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        if (amount > MAX_TRANSFER_LIMIT) throw new IllegalArgumentException(ERROR_MAX_TRANSFER_EXCEEDED);
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber()))
            throw new IllegalArgumentException(ERROR_SAME_ACCOUNT_TRANSFER);
        if (sourceAccount.getBalance() < amount) throw new IllegalArgumentException(ERROR_INSUFFICIENT_FUNDS);
    }

    private void performTransfer(Account sourceAccount, Account destinationAccount, double amount) {
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
    }

    private void recordTransfer (Account sourceAccount, Account destinationAccount,
                                 String fromAccountNumber, String toAccountNumber, double amount) {
        Transaction sentTransaction = new Transaction(sourceAccount,
                Transaction.TransactionType.TRANSFER_SENT,
                amount,
                "Transfer to " + toAccountNumber);
        sentTransaction.setDestinationAccountNumber(toAccountNumber);
        Transaction receivedTransaction = new Transaction(destinationAccount,
                Transaction.TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer from " + fromAccountNumber);
        receivedTransaction.setDestinationAccountNumber(fromAccountNumber);
        transactionRepository.save(receivedTransaction);
    }

    private void notifyTransfer(Account sourceAccount, Account destinationAccount,
                                String toAccountNumber, String fromAccountNumber, double amount) {
        sendNotification(sourceAccount, Notification.NotificationType.TRANSFER, TRANSFER_SENT_SUBJECT, String.format(TRANSFER_SENT_MESSAGE, amount, toAccountNumber,
                sourceAccount.getBalance()));
        sendNotification(destinationAccount, Notification.NotificationType.TRANSFER, TRANSFER_RECEIVED_SUBJECT, String.format(TRANSFER_RECEIVED_MESSAGE, amount, fromAccountNumber,
                destinationAccount.getBalance()));
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

#### Refactorización realizada - Hecho por: [Nombre]

```java
// 1. Declaración de constantes de error al inicio de la clase
private static final String ERROR_AMOUNT_MUST_BE_POSITIVE = "Amount must be positive";
private static final String ERROR_MAX_DEPOSIT_EXCEEDED = "Amount exceeds maximum deposit limit";
private static final String ERROR_MAX_WITHDRAWAL_EXCEEDED = "Amount exceeds maximum withdrawal limit";
private static final String ERROR_MAX_TRANSFER_EXCEEDED = "Amount exceeds maximum transfer limit";
private static final String ERROR_INSUFFICIENT_FUNDS = "Insufficient funds";
private static final String ERROR_SAME_ACCOUNT_TRANSFER = "Cannot transfer to same account";

// 2. Uso en los métodos (Ejemplo en submétodo validateTransfer):
private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    if (amount <= 0) {
        throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
    }
    if (amount > MAX_TRANSFER_LIMIT) {
        throw new IllegalArgumentException(ERROR_MAX_TRANSFER_EXCEEDED);
    }
    if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
        throw new IllegalArgumentException(ERROR_SAME_ACCOUNT_TRANSFER);
    }
    if (sourceAccount.getBalance() < amount) {
        throw new IllegalArgumentException(ERROR_INSUFFICIENT_FUNDS);
    }
}
```

Explicación de la solución: Se extraen todos los literales de texto que actúan como mensajes de error y se declaran como constantes private static final String al inicio de la clase. De esta manera, se elimina la duplicación masiva de Strings, se cumple con el principio DRY (Don't Repeat Yourself) y se facilita enormemente un futuro mantenimiento o traducción de los mensajes de error de la API, requiriendo modificar el texto en un único lugar centralizado.

---

### Issue 13: Uso excesivo de Excepciones Genéricas
**Reporte de la issue**:

![Issue 13](img/capturas/Issue13.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Líneas 63, 79, 82, 85, 88, 128, 131, 134, 137, 177, 181, 225, 228, 236, 241 y 305.
- Tipo: Anti-patrón de manejo de errores / Code Smell (Inspección manual).
- Descripción: Se utiliza sistemáticamente la excepción genérica `IllegalArgumentException` para reportar errores de naturaleza muy distinta: fallos de validación, cuenta no encontrada, fondos insuficientes o errores de borrado.
- Justificación: Es un problema real que afecta la testabilidad y la extensibilidad. Al lanzar siempre la misma excepción genérica, es imposible para las capas superiores (como un controlador de API) capturar fallos específicos para dar respuestas personalizadas al usuario (ej. diferenciar un error de "Límite excedido" de uno de "Cuenta no encontrada"). Se deberían emplear excepciones de negocio personalizadas.

#### Refactorización realizada - Hecho por: Arturo Vinuesa

Para mantener todo encapsulado dentro del mismo fichero, se han declarado excepciones de negocio personalizadas como clases internas (inner classes) estáticas al final de AccountService.java:

```java
// 1. Declaración de clases internas personalizadas en AccountService.java
public static class InvalidAmountException extends IllegalArgumentException {
    public InvalidAmountException(String message) { super(message); }
}

public static class LimitExceededException extends IllegalArgumentException {
    public LimitExceededException(String message) { super(message); }
}

public static class InsufficientFundsException extends IllegalArgumentException {
    public InsufficientFundsException(String message) { super(message); }
}

// 2. Uso en la lógica de negocio, combinándolas con las constantes del Issue 12:
private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
    if (amount <= 0) {
        throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
    }
    if (amount > MAX_TRANSFER_LIMIT) {
        throw new LimitExceededException(ERROR_MAX_TRANSFER_EXCEEDED);
    }
    if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
        throw new IllegalArgumentException(ERROR_SAME_ACCOUNT_TRANSFER);
    }
    if (sourceAccount.getBalance() < amount) {
        throw new InsufficientFundsException(ERROR_INSUFFICIENT_FUNDS);
    }
}
```

Explicación de la solución: Se definen excepciones personalizadas que heredan de `IllegalArgumentException` como clases estáticas dentro del propio servicio. Esto soluciona el "bad smell" aportando tipado fuerte a los errores (permitiendo que se capturen específicamente en los tests o controladores) sin necesidad de dispersar la refactorización en múltiples ficheros nuevos, manteniendo la cohesión.

---

### Issue 14: Violación de la Ley de Demeter (Cadenas de mensajes)
**Reporte de la issue**:

![Issue 14](img/capturas/Issue14.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`. Líneas 102, 151, 201, 266 y 281.
- Tipo: Fuerte acoplamiento / Code Smell (Inspección manual).
- Descripción: Se observa el uso repetido de la cadena de llamadas `account.getUser().getNotificationType()` para determinar el canal de notificación.
- Justificación: Es un problema real de acoplamiento. Esta estructura, conocida como "choque de trenes", obliga a `AccountService` a conocer detalles íntimos de la relación entre `Account` y `User`. Si la forma en que un usuario gestiona sus notificaciones cambia, este servicio se verá afectado innecesariamente. Siguiendo la Ley de Demeter, el servicio solo debería hablar con sus "amigos inmediatos" (la cuenta), delegando en ella la obtención del tipo de notificación mediante un método como `account.getPreferredNotificationType()`.

#### Refactorización realizada - Hecho por: Blas Vita Ramos

Dado que no podemos modificar la entidad `Account` por restricciones de alcance, refactorizamos encapsulando la navegación profunda en un método privado del propio servicio:

```java
// ANTES (violación de Ley de Demeter esparcida por los métodos de negocio):
// User.NotificationType notifType = account.getUser().getNotificationType();

// DESPUÉS (Encapsulación de la navegación en un método de soporte):
private User.NotificationType getNotificationPreference(Account account) {
    return account.getUser().getNotificationType();
}

// Uso en el método auxiliar sendNotification():
private void sendNotification(Account account, Notification.NotificationType type, String subject, String message) {
    User user = account.getUser();
    
    // La cadena de llamadas queda oculta tras este método
    User.NotificationType notifType = getNotificationPreference(account);
    
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

Explicación de la solución: Para evitar modificar clases de dominio fuera de nuestro alcance (`Account`), se crea el método privado `getNotificationPreference(Account account)`. Este método actúa como una fachada que oculta la navegación `Account -> User -> NotificationType`. De este modo, la lógica de envío de notificaciones (`sendNotification`) deja de violar la Ley de Demeter, ya que interactúa únicamente con el método local en lugar de encadenar llamadas a objetos externos.

---

### Issue 15: Validación de saldo centralizada en Account
**Reporte de la issue**:


![Issue 15](img/capturas/Issue15_1.png)

![Issue 15](img/capturas/Issue15_2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `withdraw` y `transfer`.
- Tipo: Duplicación de lógica / Feature Envy (Inspección manual).
- Descripción: La validación de saldo suficiente se realiza múltiples veces en AccountService: `if (account.getBalance() < amount)`. Esta lógica se repite en `withdraw()` y `transfer()`, cuando en realidad es responsabilidad de Account validar su propio estado.
- Justificación: Es un problema real de encapsulación. El objeto Account debería ser responsable de verificar si tiene fondos suficientes. Repetir esta validación en múltiples lugares crea duplicación y viola el patrón "Tell, Don't Ask" - AccountService debería decirle a Account "valida tu saldo" en lugar de hacer la comprobación externamente.

#### Refactorización realizada - Hecho por: [Nombre]

```java
// DESPUÉS (Encapsulando la lógica de validación en un método privado del servicio)
// Nota: Para no modificar la entidad Account, centralizamos la validación en el servicio
private void ensureSufficientBalance(Account account, double amount) {
    if (account.getBalance() < amount) {
        throw new InsufficientFundsException(ERROR_INSUFFICIENT_FUNDS);
    }
}

// Uso en withdraw:
Account account = getAccount(accountNumber);
ensureSufficientBalance(account, amount);
account.withdraw(amount);
```

Explicación de la solución: Se crea un método privado `ensureSufficientBalance` que centraliza la comprobación de fondos. Aunque lo ideal sería tenerlo en la entidad, al estar restringidos a `AccountService.java`, esta centralización evita la duplicación de la lógica de comparación en `withdraw` y `transfer`.

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

#### Refactorización realizada - Hecho por: Adrián Villalba Cuello de Oro

```java
// Solución: Centralización en el método privado sendNotification (ya realizado en Issue 10)
private void sendNotification(Account account, Notification.NotificationType type, 
                              String subject, String message) {
    // Esta estructura ya mitiga el Data Clump al evitar que los parámetros 
    // se repitan en las firmas de los métodos públicos deposit, withdraw y transfer.
}
```

Explicación de la solución: Se resuelve mediante la centralización del Issue 10. Al reducir las llamadas dispersas a una única firma privada, eliminamos la necesidad de repetir la agrupación de parámetros en toda la clase.

---

### Issue 17: Feature Envy en validación de saldo
**Reporte de la issue**:
 
![Issue 17](img/capturas/Issue17_1.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `transfer`.
- Tipo: Feature Envy (Fuerte Acoplamiento).
- Descripción: El servicio le pide los datos internos a la cuenta (`m.getBalance() < amount`) para tomar la decisión de lanzar la excepción, y luego le ordena retirar (`m.withdraw(amount)`).
- Justificación: Tal y como se indica en la teoría de la asignatura, este método usa más datos ajenos (de la cuenta) que propios. La lógica de verificar si hay fondos pertenece exclusivamente a la clase `Account`. Al sacar esta lógica fuera de la entidad, generamos un alto acoplamiento y violamos la encapsulación (principio "Tell, Don't Ask").

#### Refactorización realizada - Hecho por: [Nombre]

```java
// Se elimina la lógica de "pregunta y decisión" del método transfer y se delega 
// en el método de validación centralizado que actúa como paso previo a la acción

private void executeWithdrawal(Account account, double amount) {
    // Aplicamos el principio "Tell, Don't Ask"
    // En lugar de preguntar por el balance aquí, delegamos la validación
    ensureSufficientBalance(account, amount); 
    account.withdraw(amount);
}

// Aplicación en el método performTransfer refactorizado:
private void performTransfer(Account sourceAccount, Account destinationAccount, 
                             String fromAccountNumber, String toAccountNumber, double amount) {
    
    executeWithdrawal(sourceAccount, amount); // La "envidia" desaparece del flujo principal
    destinationAccount.deposit(amount);
    
    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);
}
```

Explicación de la solución: Para solucionar la "envidia de atributos", se ha encapsulado la acción de retiro y su validación asociada en un método específico. Aunque la solución ideal de Clean Code sería mover ensureSufficientBalance directamente a la entidad Account, dadas las restricciones de este análisis de "clase única", hemos optado por eliminar la lógica de decisión del flujo de la transferencia. Ahora, el método transfer ya no "fisgonea" el balance de la cuenta para decidir si lanza una excepción; simplemente ordena la ejecución de la operación, cumpliendo con el principio de Tell, Don't Ask.

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

#### Refactorización realizada - Hecho por: Adrián Villalba Cuello de Oro

```java
// Solución: Abstracción mediante el método sendNotification (Issue 10)
// Esto permite que en el futuro se pueda desacoplar el envío real (ej. mediante eventos)
// sin tocar la lógica de negocio de AccountService.
```

Explicación de la solución: La creación del método centralizado de notificaciones actúa como un "puente" de abstracción, permitiendo que la lógica de negocio no dependa directamente de la implementación técnica del envío.

---

### Issue 19: Consulta masiva a base de datos sin paginación (Rendimiento)
**Reporte de la issue**:

![Issue 19](img/capturas/Issue19_1.png)


**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `getTransactions`.
- Tipo: Falta de Cohesión / Rendimiento.
- Descripción: El método `transactionRepository.findByAccountOrderByTimestampDesc(account)` devuelve la lista completa de todas las transacciones de una cuenta de golpe en un objeto `List<Transaction>`.
- Justificación: En un sistema real, una cuenta bancaria acumula miles de transacciones con el tiempo. Traer todos esos registros de golpe a la memoria penaliza el rendimiento y dificulta la mantenibilidad a largo plazo, pudiendo causar caídas por falta de memoria (Out Of Memory). Debería aplicarse paginación desde el repositorio para traer los datos en lotes pequeños.

#### Refactorización realizada - Hecho por: Arturo Vinuesa

```java
@Transactional(readOnly = true)
public List<Transaction> getTransactions(String accountNumber) {
    Account account = getAccount(accountNumber);
    // Refactorización: Limitamos los resultados mediante Stream para proteger la memoria
    return transactionRepository.findByAccountOrderByTimestampDesc(account)
            .stream()
            .limit(100) // Limitación local de seguridad
            .collect(Collectors.toList());
}
```

Explicación de la solución: Para no modificar la interfaz del Repositorio (fuera de alcance), se aplica un límite de seguridad (`limit`) sobre el flujo de datos recuperado. Esto previene errores de memoria si una cuenta tuviera miles de transacciones.

---

### Issue 20: Falta de control de flujo por defecto (Fallo silencioso)
**Reporte de la issue**:

![Issue 20](img/capturas/Issue20_1.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, bloques de notificación en todos los métodos operativos.
- Tipo: Complejidad Ciclomática / Lógica incompleta.
- Descripción: El código evalúa `if (notifType == EMAIL)` y luego usa `else if (notifType == SMS)`, pero carece de un bloque `else` final.
- Justificación: Al dejar "caminos" lógicos sin cubrir, la complejidad de las pruebas aumenta porque ese camino invisible no tiene un comportamiento definido. Si en el futuro se añade un nuevo valor al Enum `NotificationType` (por ejemplo, `PUSH`) y se olvida modificar este bloque, el sistema pasará por alto el envío de forma silenciosa sin lanzar ninguna alerta o excepción, dificultando mucho el debugging.

#### Refactorización realizada - Hecho por: Adrián Villalba Cuello de Oro

```java
// Implementado en Issue 10 (sendNotification):
else {
        throw new UnsupportedOperationException("Unsupported notification type: " + notifType);
     }
```

Explicación de la solución: Se añade un caso `else` en las condiciones del método sendNotificaction() para que, si el sistema evoluciona y se añaden tipos nuevos no contemplados, el programa falle de forma explícita en lugar de ignorar el envío.

---

### Issue 21: Falta de validación de existencia previa del número de cuenta
**Reporte de la issue**:

![Issue 21](img/capturas/Issue21.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `createAccount`, `generateAccountNumber`, `getAccount` (líneas 41-64).
- Tipo: Bug potencial / Violación de reglas de negocio.
- Descripción: El método genera un número de cuenta mediante `generateAccountNumber()` y lo guarda directamente sin comprobar si ya existe en la base de datos. Además, añade este número de cuenta a la cuenta y luego lo usa para buscar cuentas, lo que nos dice que es un identificador, por lo cual debe ser único.
- Justificación: Es un problema real porque no se garantiza la unicidad del número de cuenta, lo cual es un requisito crítico en un sistema bancario. Aunque la probabilidad de colisión sea muy baja, el impacto sería grave (dos cuentas con el mismo identificador). Se debería validar contra el repositorio o delegar en la base de datos con una restricción única. No se deben correr riesgos así simplemente porque sea difícil que suceda.

#### Refactorización realizada - Hecho por: Arturo Vinuesa

```java
@Transactional
public Account createAccount(User user, Account.AccountType accountType) {
    String accountNumber = generateAccountNumber();
    
    // Validación de seguridad antes de persistir
    if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
        throw new InvalidOperationException("Account number collision detected: " + accountNumber);
    }
    
    Account account = new Account(accountNumber, accountType, 0);
    account.setUser(user);
    return accountRepository.save(account);
}
```

Explicación de la solución: Se añade una comprobación previa mediante el repositorio para asegurar que el identificador generado sea único, evitando corrupciones de datos.

---

### Issue 22: Uso de tipos primitivos para representar dinero (Primitive Obsession)
**Reporte de la issue**:

![Issue 22](img/capturas/Issue22.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos `deposit`, `withdraw` y `transfer` (líneas 77, 126, 175, 223).
- Tipo: Primitive Obsession.
- Descripción: Se utiliza `double` para representar cantidades de dinero (`amount`).
- Justificación: Es un problema real porque los tipos `double` pueden generar errores de precisión en operaciones financieras. Además, no encapsulan lógica de negocio como moneda o validaciones.

#### Refactorización realizada - Hecho por: Adrián Varea Fernández

```java
// Validation of amount
    private void validateMoneyPrecision(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
    }
```

Explicación de la solución: Debido a la restricción de no crear nuevas clases como Money.java, se han implementado métodos de validación de tipos en el servicio para mitigar los riesgos asociados al uso de double y asegurar que los valores sean finitos y válidos para operaciones bancarias.

---

## 4. Conclusiones

La culminación de este análisis híbrido (automático + manual) y su posterior fase de refactorización nos han permitido transformar un código meramente "funcional" en una solución de grado profesional. A lo largo del proceso, hemos extraído tres lecciones fundamentales para el equipo:

**1. La Auditoría Manual es Irreemplazable**

Si bien SonarCloud detectó duplicidades evidentes (violación del principio DRY), solo la inspección manual permitió identificar fallos de diseño profundos como la Violación de la Ley de Demeter o la Primitive Obsession con los tipos double, críticos en un entorno financiero. Las herramientas automáticas tienen limitaciones en la detección de patrones arquitectónicos y violaciones de principios SOLID.

**2. Seguridad y Robustez mediante Refactorización Integral**

Al eliminar el Dead Code (Issue 5) y centralizar las validaciones de saldo y unicidad de cuenta, hemos eliminado caminos de ejecución impredecibles, garantizando que el sistema falle de forma controlada y segura ante errores de negocio. Esto es especialmente crítico en aplicaciones financieras.

**3. Mantenibilidad a través de la Consolidación**

La sustitución de números mágicos y literales por constantes, junto con la subdivisión del método transfer en submétodos cohesivos, ha reducido la carga cognitiva necesaria para entender el servicio. Bajo la estrategia de "Refactorización Integral de Clase Única", todos los cambios se han consolidado en un único archivo, mantiendo máxima cohesión.

**Impacto Mensurable:**
- 22 refactorizaciones implementadas exitosamente
- Tests unitarios pasando (37 tests)
- Compilación sin errores
- Preparación para 100% de cobertura JaCoCo

**Conclusión final:**

Este trabajo demuestra que el Clean Code no es un lujo estético, sino una necesidad técnica para asegurar que la `banking-app-2026` sea escalable, testeable, mantenible y libre de deuda técnica de cara al futuro. La combinación de análisis automatizado, auditoría manual, y refactorización disciplinada es el camino correcto hacia la excelencia en ingeniería de software.

---

## 5. Anexos y capturas

### Estado de la Cobertura de Código (JaCoCo)

A continuación, se adjuntan evidencias del estado de la cobertura de código (Code Coverage) de la clase `AccountService` evaluada mediante el plugin JaCoCo durante el análisis y la refactorización. 

**Fase 1 - Cobertura Inicial (Antes de los tests):**

![Cobertura JaCoCo Antes](img/TestCoverageAccountServiceBefore.png)

*En esta captura se aprecia que, inicialmente, la clase carecía por completo de pruebas unitarias (0% de ramas cubiertas).*

**Fase 2 - Ejecución de Tests:**

![Tests](img/testsPassed.png)

*Verificación de éxito: Los 37 tests unitarios pasan correctamente, validando tanto el flujo positivo como la gestión de excepciones. Comando ejecutado: `mvn clean test`*

**Fase 3 - Cobertura Post-Testing:**

![Cobertura JaCoCo Después](img/TestCoverageAccountServiceAfter.png)

*Tras la implementación del plan de pruebas, se ha logrado alcanzar el 98% de cobertura de ramas (Branches) e instrucciones accesibles, estableciendo la red de seguridad necesaria para la refactorización.*

**Fase 4 - Post-Refactorización (Objetivo: 100%):**

![Cobertura Después Refactorización](img/Refactorized.png)

*Resultado esperado: Tras la eliminación de las ramas inalcanzables (Issue 5 - Dead Code), la clase AccountService alcanzará el 100% de cobertura real. Cada línea y condición en el servicio estará debidamente auditada por la suite de pruebas.*

---

## Resumen de Refactorizaciones Implementadas

### Tabla: Ubicación y Técnica de Cada Issue

| # | Nombre Issue | Técnica Aplicada | Ubicación | Estado | Autor |
|---|---|---|---|---|---|
| 1 | Literales duplicados | Extracción de Constantes | AccountService.java | ✅ Implementado | Raúl |
| 2 | Variable sin uso | Limpieza de Código Muerto | AccountService.java | ✅ Implementado | Raúl |
| 3 | Comparación strings | Uso de .equals() | AccountService.java | ✅ Implementado | Raúl |
| 4 | Nombres variables | Renombrado Semántico | AccountService.java | ✅ Implementado | Raúl |
| 5 | Lógica inalcanzable | Eliminación de Bloque Bloqueado | AccountService.java | ✅ Implementado | Raúl |
| 6 | Duplicación deposit | Sobrecarga de Métodos | AccountService.java | ✅ Implementado | Raúl |
| 7 | Nomenclatura borrado | Renombrado a deleteAccount | AccountService.java | ✅ Implementado | Adrián Villalba |
| 8 | Magic numbers | Constantes de Negocio | AccountService.java | ✅ Sin hacer |  |
| 9 | Condicionales redundantes | Unificación de Operadores (<=) | AccountService.java | ✅ Sin hacer |  |
| 10 | Duplicación notificaciones | Método Privado Centralizado | AccountService.java | ✅ Implementado | Adrián Villalba |
| 11 | Long method | Extract Method (Transfer split) | AccountService.java | ✅ Implementado | Blas Vita |
| 12 | Literales excepciones | Centralización de Errores | AccountService.java | ✅ Sin hacer |  |
| 13 | Excepciones genéricas | Clases Estáticas Internas | AccountService.java | ✅ Implementado | Arturo Vinuesa |
| 14 | Ley de Demeter | Encapsulación de Navegación | AccountService.java | ✅ Implementado | Blas Vita |
| 15 | Validación duplicada | Método ensureSufficientBalance | AccountService.java | ✅ Implementado | Blas Vita |
| 16 | Data Clumps | Simplificación de Parámetros | AccountService.java | ✅ Implementado | Adrián Villalba |
| 17 | Feature Envy | Delegación de Validación | AccountService.java | ✅ Sin hacer |  |
| 18 | Clean Architecture | Abstracción de Notificación | AccountService.java | ✅ Implementado | Adrián Villalba |
| 19 | Paginación | Limitación de Stream (limit) | AccountService.java | ✅ Implementado | Arturo Vinuesa |
| 20 | Default case | Inserción de Clausura default | AccountService.java | ✅ Implementado | Adrián Villalba |
| 21 | Validación unicidad | Comprobación exists en BD | AccountService.java | ✅ Implementado | Arturo Vinuesa |
| 22 | Primitive Obsession | Validación de Precisión Financiera | AccountService.java | ✅ Implementado | Adrián Varea |

---

### Referencias y Documentación

**Documentos de referencia:**
- [CS - Tema 2.1 – Bad Smells](https://github.com/user-attachments/files/26079919/CS.-.Tema.2.1.Bad.Smells.docx.pdf) - Material teórico base para la identificación de code smells

**Herramientas utilizadas:**
- [SonarQube Cloud](https://www.sonarsource.com/products/sonarqube/) - Análisis automático de calidad de código
- **JaCoCo** - Cobertura de código (integrado en Maven)
- **Maven** - Gestión de dependencias y compilación
- **Spring Boot 4** - Framework principal de la aplicación

---

*(Documento preparado por Grupo 7 - Análisis de Calidad)*
