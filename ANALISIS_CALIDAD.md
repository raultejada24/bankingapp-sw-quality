# Grupo 7
Integrantes: Adrián Varea Fernández, Adrián Villalba Cuello de Oro, Arturo Vinuesa Domínguez, Blas Vita Ramos, Gonzalo Andrés Zurdo Patiño, Raúl Tejada Merinero

# Tarea 1: Análisis de Calidad del Código

## Captura de Pantalla del Overview de SonarQube

![Overview SonarQube](img/capturas/OverView.png)
![Overview SonarQube](img/capturas/OverView2.png)

En las capturas superiores se muestra el estado general del proyecto tras el primer escaneo. Se pueden observar las métricas de mantenibilidad (Code Smells), fiabilidad y seguridad antes de aplicar cualquier corrección.

---

## Estructura de Ejemplo para nuevos Issues
(Copiar y pegar este bloque para añadir más detecciones)

### Issue X: Título del problema
**Reporte de la issue**:
![Issue X](img/capturas/IssueX.png)

**Explicación del mal olor detectado**:
- Ubicación: archivo y línea.
- Tipo de problema: categoría de Sonar o inspección manual.
- Descripción: qué está pasando en el código.
- Justificación: razonamiento de por qué se considera un fallo y si es un falso positivo o no.

---

## Análisis de Calidad - Issues detectados

### Issue 1: Duplicación del literal "Deposit Confirmation"
**Reporte de la issue**:
![Issue 1](img/capturas/deposit-confirmation-varible-1.png)
![Issue 1](img/capturas/deposit-confirmation-varible-2.png)
![Issue 1](img/capturas/deposit-confirmation-varible-3.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 107, 114, 156, 163.
- Tipo: Code Smell (Critical).
- Descripción: El literal "Deposit Confirmation" se repite cuatro veces en el código para definir el asunto de las notificaciones, tanto para el canal de Email como para el de SMS.
- Justificación: Es un problema real de mantenibilidad. Al tener el mismo texto "hardcodeado" en varios puntos, cualquier cambio futuro en el mensaje obligaría a modificar el código en muchos lugares, aumentando el riesgo de olvidar alguno y generar algún tipo de inconsistencia. Lo adecuado sería extraer este valor a una constante única para poder centralizar el mensaje y facilitar su gestión.

### Issue 2: Variable "seccondAccount" sin uso en AccountService
**Reporte de la issue**:
![Issue 2](img/capturas/Issue4.png)
![Issue 2](img/capturas/duplicate-variable.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 185.
- Tipo: Code Smell (Minor).
- Descripción: Se ha dejado declarada una variable llamada seccondAccount que no hace nada en el método de retiro.
- Justificación: Es un problema real aunque de baja prioridad. Es simplemente código muerto que sobra. Al leer el código, da la sensación de que falta algo por programar o que se ha quedado ahí después de un borrador previo, por lo que debería eliminarse para no confundir.

### Issue 3: Las cadenas de texto no se comparan usando equals()"
**Reporte de la issue**:
![Issue 3](img/capturas/equals-problem-1.png)
![Issue 3](img/capturas/equals-problem-2.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 235.
- Tipo: Bug (Major).
- Descripción: Se está utilizando el operador de igualdad referencial "==" para comparar dos números de cuenta que son de tipo String.
- Justificación: Es un problema real y grave. El operador "==" comprueba si ambos objetos son la misma instanciaen memoria, no si tienen el mismo contenido, por lo que en este caso la comparación podría devolver false aunque los números sena idénticos. Lo que habría que hacer es cambiar esta línea por "m.getAccountNumber().equals(o.getAccountNumber())".

### Issue 4: Nombres de variables no descriptivos en AccountService
**Reporte de la issue**:
![Issue 4](img/capturas/Issue8.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 231 y 232.
- Tipo: Mantenibilidad (Nombres crípticos).
- Descripción: En el método de transferencia se usan las letras "m" y "o" para referirse a las cuentas de origen y destino.
- Justificación: Es un problema real. El uso de variables de una sola letra obliga a cualquier programador que lea el código a tener que adivinar qué cuenta es cuál. Lo correcto sería usar nombres como "sourceAccount" y "destinationAccount" para que el código se explique por sí solo sin necesidad de comentarios.

### Issue 5: Validaciones de negocio redundantes e inalcanzables
**Reporte de la issue**:
![Issue 5](img/capturas/Issue9.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, líneas 87 a 89.
- Tipo: Lógica redundante (Dead Code).
- Descripción: Se comprueba si el importe es mayor de 10.000 y, justo después, si es mayor de 50.000 para lanzar el mismo error.
- Justificación: Es un problema de lógica real. Si alguien intenta ingresar 60.000, el programa saltará en el primer "if" (el de 10.000) y nunca llegará a evaluar el segundo. Esto hace que el código sea confuso y parezca que los límites de seguridad no están bien definidos o que se ha copiado y pegado el código sin revisarlo.

### Issue 6: Duplicación de lógica en los métodos de depósito
**Reporte de la issue**:
![Issue 6](img/capturas/Issue10.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, métodos deposit (líneas 76 a 169).
- Tipo: Violación del principio DRY (Don't Repeat Yourself).
- Descripción: Existen dos métodos para depositar dinero que repiten exactamente las mismas validaciones y la misma lógica de guardado y notificación.
- Justificación: Es un problema real de duplicación. Si en el futuro el banco decide cambiar una regla de depósito, el desarrollador tendrá que modificar dos métodos distintos. El método corto (sin descripción) debería simplemente llamar al método largo pasando una descripción por defecto, evitando así tener el código duplicado.

### Issue 7: Nomenclatura inadecuada en métodos de borrado
**Reporte de la issue**:
![Issue 11](img/capturas/Issue11.png)

**Explicación del mal olor detectado**:
- Ubicación: `src/main/java/es/codeurjc/service/AccountService.java`, línea 301.
- Tipo: Diseño de API / Mantenibilidad.
- Descripción: El método para eliminar una cuenta se llama simplemente "rm".
- Justificación: Es un mal olor claro. Aunque "rm" es un comando conocido en sistemas Linux, en el contexto de un servicio Java de una aplicación bancaria se deben usar nombres verbales completos como "deleteAccount". Las abreviaturas crípticas reducen la legibilidad de la arquitectura del sistema.

---

**Refactorización**
(Se realizará en la Tarea 3)
