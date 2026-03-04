# Grupo 7
Adrián Varea Fernández, Adrián Villalba Cuello de Oro, Arturo Vinuesa Domínguez, Blas Vita Ramos, Gonzalo Andrés Zurdo Patiño, Raúl Tejada Merinero

# Tarea 1: Análisis de Calidad del Código

## Captura de Pantalla del Overview de SonarQube

![Overview SonarQube](img/capturas/OverView.png)
![Overview SonarQube](img/capturas/OverView2.png)
Dashboard principal tras el primer análisis, donde se observan las métricas de mantenibilidad, fiabilidad y seguridad.

---

## Análisis de Calidad - Issues 

### Issue 1: Literales de cadena duplicados en LoanController

**Reporte de la issue**:
![Issue 1](img/capturas/Issue1.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/controller/LoanController.java`, a partir de la línea 43.
- **Tipo:** Code Smell (Critical) - Rule java:S1192.
- **Descripción:** El literal de cadena `"error"` aparece repetido 9 veces a lo largo del controlador, tanto para nombres de atributos del modelo (`model.addAttribute("error", ...)`) como para nombres de vistas de retorno (`return "error";`).
- **Justificación:** Es un **problema real**. El uso de "Magic Strings" (cadenas mágicas) viola el principio de diseño **DRY (Don't Repeat Yourself)**. 
  - **Mantenibilidad:** Si en el futuro se decide cambiar el nombre de la vista de error o la clave del atributo, el desarrollador tendría que buscar y reemplazar manualmente las 9 ocurrencias, lo cual es propenso a errores tipográficos.
  - **Fragilidad:** Un error de escritura en una sola de las repeticiones (por ejemplo, escribir `"erorr"`) provocaría un fallo en tiempo de ejecución difícil de detectar mediante compilación simple.
  - **Solución recomendada:** Se debe definir una constante de clase: `private static final String ERROR_VIEW = "error";` y referenciarla en todos los puntos necesarios.

### Issue 2: Duplicación de literales de éxito en LoanController

**Reporte de la issue**:
![Issue 2](img/capturas/Issue2.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/controller/LoanController.java`, líneas 57, 91 y 110.
- **Tipo:** Code Smell (Critical) - Rule java:S1192.
- **Descripción:** Se utiliza la cadena de texto `"success"` en 3 ocasiones distintas para añadir atributos de redirección (`addFlashAttribute`).
- **Justificación:** Se trata de un **problema real**. Aunque se repite menos veces que el literal "error", sigue introduciendo una dependencia innecesaria de una cadena de texto manual. Al tratarse de una clave de mensaje que probablemente se use en las plantillas (Mustache/HTML) para mostrar alertas verdes al usuario, cualquier error al escribir la palabra en un nuevo método rompería la comunicación entre el controlador y la vista.
- **Solución recomendada:** Centralizar el literal en una constante `private static final String SUCCESS_ATTR = "success";`.

### Issue 3: Duplicación de rutas de redirección ("redirect:/loan/manage")

**Reporte de la issue**:
![Issue 3](img/capturas/Issue3.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/controller/LoanController.java`, líneas 96, 100, 114 y 118.
- **Tipo:** Code Smell (Critical) - Rule java:S1192.
- **Descripción:** La ruta de redirección completa `"redirect:/loan/manage"` aparece escrita a mano 4 veces dentro de los métodos de aprobación y rechazo de préstamos.
- **Justificación:** Es un **problema real** y un mal olor de acoplamiento. Las rutas de navegación son elementos críticos del sistema; si la estructura de las URLs del banco cambia (por ejemplo, de `/loan/manage` a `/admin/loans`), habría que buscar y modificar cada String manualmente. 
- **Impacto:** Este tipo de duplicación aumenta drásticamente las posibilidades de dejar "enlaces rotos" tras una refactorización. La mejor práctica es definir la ruta en una constante o usar un sistema de resolución de nombres.

### Issue 4: Variable local no utilizada (Dead Code) en AccountService

**Reporte de la issue**:
![Issue 4](img/capturas/Issue4.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/service/AccountService.java`, Línea 185.
- **Tipo:** Code Smell (Minor) - Rule java:S1481.
- **Descripción:** Se declara la variable `Account seccondAccount;` pero nunca se le asigna un valor ni se utiliza en el resto del método `withdraw`.
- **Justificación:** Es un **problema real**. El "código muerto" o variables huérfanas ensucian el código, confunden a otros desarrolladores que puedan pensar que falta lógica por implementar y aumentan la carga cognitiva innecesariamente. Es un resto de código que debe ser eliminado para limpiar la clase.

### Issue 5: Llamada a método @Transactional vía 'this' (Proxy Bypass)

**Reporte de la issue**:
![Issue 5](img/capturas/Issue5.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/service/loan/LoanService.java`, Línea 104.
- **Tipo:** Code Smell (Critical) - Rule java:S6809.
- **Descripción:** Dentro del método `approveLoan`, se llama directamente a `rejectLoan(loanId, ...)` usando la referencia interna.
- **Justificación:** Es un **problema real y técnico grave**. Spring gestiona las transacciones (`@Transactional`) mediante proxies. Cuando un método llama a otro de la misma clase usando `this` o de forma directa, el proxy se salta y la configuración transaccional del segundo método (como el rollback en caso de error) **no se aplica**. Esto puede causar inconsistencias en la base de datos si el rechazo del préstamo falla.

### Issue 6: Duplicación de mensaje de error ("Loan not found") en LoanService

**Reporte de la issue**:
![Issue 6](img/capturas/Issue6.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/service/loan/LoanService.java`, Líneas 90, 151, 187 y 274.
- **Tipo:** Code Smell (Critical) - Rule java:S1192.
- **Descripción:** El literal de texto `"Loan not found"` para las excepciones se repite 4 veces en diferentes métodos del servicio.
- **Justificación:** Es un **problema real**. Al igual que con los controladores, repetir mensajes de error exactos es ineficiente. Si el equipo de UX o negocio pide cambiar el mensaje a "Préstamo no localizado", habría que cambiarlo en 4 sitios. 
- **Solución:** Se debería definir una constante `private static final String LOAN_NOT_FOUND_MSG = "Loan not found";`.

### Issue 7: Constructor con exceso de parámetros (Long Parameter List) en User

**Reporte de la issue**:
![Issue 7](img/capturas/Issue7.png)

**Explicación del mal olor detectado**:
- **Ubicación:** `src/main/java/es/codeurjc/model/User.java`, Línea 59.
- **Tipo:** Code Smell (Major) - Rule java:S107.
- **Descripción:** El constructor de la entidad `User` recibe 10 parámetros simultáneamente.
- **Justificación:** Es un **problema real de diseño**. Los métodos con más de 7 parámetros son difíciles de usar y mantener (mal olor: *Brain Overload*). Es muy fácil equivocarse al pasar los datos (por ejemplo, intercambiar el orden del email y el teléfono) porque casi todos son de tipo `String`.
- **Solución:** Se recomienda usar el patrón de diseño **Builder** o agrupar parámetros relacionados en objetos de valor (como un objeto `PersonalInfo`).



---

**Refactorización**
(Se realizará en la Tarea 3)