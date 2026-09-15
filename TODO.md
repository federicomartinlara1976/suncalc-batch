# Pendiente

Estas son las tareas pendientes por hacer, ordenadas por versión y fecha

- **Versión 1.0.2**

    - 10/07/2026
      - El endpoint de obtención del registro con fecha actual deberá recogerlo de la base de datos NoSQL si ya ha recuperado el registro, para evitar hacer la recuperación externa **(Completado - 11/07/2026)**
      
    - 11/07/2026
      - Hacer endpoint para consultar por una fecha en concreto **(Completado - 11/07/2026)**
      - Hacer endpoint para consultar por un rango de fechas. El resultado será una colección de varios elementos **(Completado - 12/07/2026)**

- **Versión 1.0.3**

    - 13/09/2026
      - Endpoint y tarea para recuperar datos de un mes y año concretos **(Completado 15/09/2026)**
      
- **Versión 1.0.4**

    - 14/09/2026
      - Eliminar código deprecado y todas las dependencias que no se utilicen **(Completado 15/09/2026)**
      
    - 15/09/2026
      - Endpoint y tarea para recalcular diferencias para 1 año en concreto, si el año es el presente, hasta la fecha actual
      - El escuchador de eventos bloquea hasta finalizar la tarea. Utilizar colas de RabbitMQ para desacoplar.
      