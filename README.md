# suncalc-batch
Recoge datos de efemérides solares

Se realiza mediante tareas programables de manera periódica.

## Instalar paquete de astronomía

1. Entrar en el contenedor para administrar con esta línea de comandos:
   
   docker exec -u root -it suncalc-batch /bin/bash
   
2. Una vez abierta la consola de administración, usar los siguientes comandos:

   - mkdir git && cd git
   - git clone https://github.com/federicomartinlara1976/octave.git
   - cd octave	
   - cd paquetes
   - cd astronomia
   - make -f Makefile-6.4.0
   - make install -f Makefile-6.4.0
   - cd ../
   - octave
   - octave:1> pkg install "symbolic-3.2.1.tar.gz"
   - octave:2> pkg install "astronomia-1.2.0.tar.gz"
   - octave:3> quit
