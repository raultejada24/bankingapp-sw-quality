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
// Se muestra exactamente el mismo código de la imagen, pero eliminando la variable adicional seccondAccount
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
//Se sustituye m y o como nombres de las variables que representan las cuentas de origen y de destino por los nombres sourceAccount para la variable de la cuenta de origen y destinationAccount para la variable de la cuenta de destino

@Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > 20000) {
            throw new IllegalArgumentException("Amount exceeds maximum transfer limit");
        }

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
Explicación de la solución: Se sustituyen los nombres de las variables correspondientes a la cuenta de origen y de destino (`m` y `o` respectivamente) por nombres que permitan identificar y localizar fácilmente cada una de estas variables y que cada una defina de manera clara lo que representa (en este caso, `sourceAccount` para la cuenta de origen y `destinationAccount`para la cuenta destino).

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

        // Se ha eliminado la condición que compureba si el importe es mayor de 50000

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.
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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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

```java
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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

```java
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

---

### Issue 15: Validación de saldo duplicada entre capas
**Reporte de la issue**:

![Issue 15_1](img/capturas/Issue15_1.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, método `withdraw` (líneas 188-190) y `src/main/java/es/codeurjc/model/Account.java`, método `withdraw` (líneas 128-130).
- Tipo: Código duplicado / Diseño de capas.
- Descripción: La comprobación de fondos insuficientes (`if (balance < amount)`) existe tanto en el servicio como dentro del propio modelo. Además, la clase `Account` expone el método `hasSufficientBalance(amount)` que no se usa en ningún punto del código.
- Justificación: Es un problema real de diseño. La duplicación de validaciones entre capas genera inconsistencias: si las reglas cambian (por ejemplo, se permite un pequeño descubierto), hay que recordar modificar dos sitios a la vez. La responsabilidad de la validación de estado interno de la cuenta debería residir únicamente en el modelo, y el servicio debería confiar en ella.
 
#### Refactorización realizada

```java
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.
 
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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
- Justificación: Siguiendo los principios de Clean Architecture, las reglas de negocio no deben depender de los detalles técnicos. Al enviar un email (que es una operación de red lenta y propensa a fallos) dentro de una transacción de base de datos abierta, acoplamos fuertemente el rendimiento de la base de datos a la velocidad del servidor de correo, lo que penaliza enormemente la escalabilidad del sistema.

#### Refactorización realizada

```java
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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

```java
Insertar captura o código corregido aquí
```
Explicación de la solución: Insertar breve explicación de la solución aquí.

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

**Cobertura Final (Tras implementar AccountServiceTest):**
*(Nota para el grupo: Añadir aquí la captura "TestCoverageAccountServiceAfter.png" cuando lleguéis al 100%)*
![Cobertura JaCoCo Después](img/TestCoverageAccountServiceAfter.png)
*Tras el reparto y la implementación del plan de pruebas, se ha logrado alcanzar el 100% de cobertura de ramas (Branches) e instrucciones accesibles, estableciendo la red de seguridad necesaria para la refactorización.*

---

### Referencias y Documentación

- [CS - Tema 2.1 – Bad Smells.docx.pdf](https://github.com/user-attachments/files/26079919/CS.-.Tema.2.1.Bad.Smells.docx.pdf)
- [Herramienta SonarQube Cloud](https://www.sonarsource.com/products/sonarqube/?s_campaign=SQ-APJ-1-Japan-Brand&s_content=Languages&s_term=sonarcloud&s_category=Paid&s_source=Paid%20Search&s_origin=Google&cq_src=google_ads&cq_cmp=23600038948&cq_con=200395503824&cq_term=sonarcloud&cq_med=&cq_plac=&cq_net=g&cq_pos=&cq_plt=gp&gad_source=1&gad_campaignid=23600038948&gbraid=0AAAAAC0fKmoueJlSX5zp_yyaBo4bekNSW&gclid=Cj0KCQjwmunNBhDbARIsAOndKpk0FsvHNiaBPCESgxmWNTwthCKwfppVPAmYKkqmSOCt1ZCuoKNGCb0aAgbUEALw_wcB)

---

*(Documento preparado por Grupo 7)*
