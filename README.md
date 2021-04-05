# Sistemas Distribuidos
## Práctica 1 - Microservicios
### Objetivos de la práctica:
Implementación de una arquitectura de Microservicios (distribuidos) con el objetivo de
aplicar conceptos de:
- Programación cliente/servidor,
- Clustering, Load balancing, Fail-over.
- Alta Disponibilidad (High Availability, HA) mediante tolerancia a Fallos
(Fault-Tolerance)
- Análisis de transparencia.
- Servicios con múltiples lenguajes (via gRPC)
- Discovery de Servicios (con etcd y con Kubernetes)
- Escalabilidad (mediante deployments de Kubernetes)
- Deployments basados en containers (Docker/Kubernetes)

### Iteración 1 - gRPC - Protocol Buffer - Docker
#### Fecha de Muestra de Avance: 1/Abr/2019
Fecha de Entrega: 8/Abr/2019
Objetivo de la iteración:
Implementación de una arquitectura de Microservicios (distribuidos) con el objetivo de
aplicar los conceptos de:
- Programación cliente/servidor,
- Clustering, Load balancing, Fail-over.
- Alta Disponibilidad (High Availability, HA)
- Tolerancia a Fallos (Fault-Tolerance)
- Análisis de transparencia.
- Servicios con múltiples lenguajes.
- Discovery de Servicios
- Escalabilidad
- Deployments basados en containers (Docker)

a) Implementar los siguientes servicios utilizando gRPC y Protocol Buffers:


- GeoService - Servicio para obtener información geográfica. Debe permitir las
siguientes operaciones:
- Obtener la lista de países
- Obtener la lista de provincias/estados de un país
- Obtener la lista de localidades de una provincia/estado
- Dado un IP, devolver el país/provincia al que pertenece, delegando la
operación en el servicio web externo. Los resultados para un IP deben ser
cacheados en el servicio.
  

- AuthService - Servicio de autenticación. Debe implementar una operación que
recibe un mail/password y devuelve el resultado de la autenticación (ok, failed).
Puede embeber una lista de usuarios/password ficticios en el servicio.


Base de datos de Ciudades:
Puede utilizar alguna base de datos CSV o JSON como por ejemplo:
https://datahub.io/core/world-cities


Los datos necesarios para el funcionamiento pueden estar embebidos en el programa (por
ejemplo en un archivo CSV).
S

ervicios de “IP to Location”:
Para obtener el país al que pertenece un IP, puede utilizar alguno de los servicios “IP to
Location” gratuitos que existen. Por ejemplo:
https://ipapi.co/8.8.8.8/json/
http://ipwhois.app/json/8.8.8.8


Los servicios deben funcionar como containers de docker.


b) Replicar los servicios en al menos dos máquinas (puede utilizar dos containers de docker para simularlo) y proveer la siguiente funcionalidad:


Implementar un programa cliente que recibe una lista de IPs y devuelve el país y la lista de
provincias y estados de ese país.

El programa debe leer la lista de IPs de los servicios desde un archivo o recibirla como
parámetros (por ejemplo geoservices=10.0.1.23,10.0.2.12) y debe balancear la carga entre
los servicios disponibles (balanceo de carga del lado del cliente).

El servicio debe seguir funcionando aún cuando alguno de los servicios individuales no lo
hiciera.


### Responder el Siguiente Cuestionario:
1) ¿El servicio es escalable ? ¿ Qué ocurre con la escalabilidad en tamaño, geográfica y
administrativa ? ¿ Qué límites tiene ?
2) ¿ Qué técnicas utilizó para mejorar la escalabilidad ?
3) Explicar si se logra o no los siguientes tipos de transparencia (Ubicación, Migración) y
cómo se logra.
4) Si una instancia del servicio falla, ¿ funciona el fail-over ?, ¿ Cómo ?
5) Con respecto a los principios las arquitecturas SOA. ¿ Cómo se representa el contrato del
servicio ? ¿ Qué puede decir con respecto al encapsulamiento ?

### Referencias
GRPC con Protocol Buffers (Google)
http://www.grpc.io/
http://www.grpc.io/docs/
https://developers.google.com/protocol-buffers/ (protobuf)
https://developers.google.com/protocol-buffers/docs/tutorials (protobuf tutorial, multi
langs)
https://scalapb.github.io/ (scala)
https://github.com/grpc/grpc-java/blob/master/examples/ (java examples)
Docker
https://docs.docker.com/
