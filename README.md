# qmusic

## Reproductor web

En `index.html`, selecciona un álbum del catálogo para abrir su detalle con portada,
artista, fecha de lanzamiento, etiquetas y canciones. Desde allí puedes reproducir
o descargar canciones y navegar a los álbumes del artista. Usa **Volver al catálogo**
para regresar conservando la búsqueda y la página actual.

En las fuentes que permiten importar, abre el menú de tres puntos de la portada
y selecciona **Importar álbum** para solicitar su importación.

## Canal de importacion

Las importaciones usan Reactive Messaging: `TdlMusicProvider` envia mensajes con
`MutinyEmitter` al canal interno `album-import`, consumido por `LclImportService`
mediante `@Incoming`. No requiere un broker ni configuracion de conectores.
El envio espera el acknowledgment del consumidor al finalizar la persistencia;
los fallos se propagan a la solicitud HTTP.

El canal interno no es durable y no recupera mensajes tras un reinicio.
Para usar un broker externo se debe agregar su extension y configurar canales
de salida y entrada distintos conectados al mismo destino, junto con la
serializacion y las politicas de entrega correspondientes.

## Busqueda semantica de artistas

El MCP `search_artists` usa el modelo `text-embedding-3-small` de OpenAI,
solicitando vectores de 384 dimensiones. Configura `OPENAI_API_KEY` en el
entorno de la aplicacion. Las consultas y los nombres y biografias de artistas
se envian a OpenAI; las solicitudes requieren acceso a internet y tienen costo.
Al iniciar, la aplicacion calcula y persiste los embeddings faltantes de la
coleccion `artists`; los artistas importados posteriormente se embeben al
guardarse. El indice vectorial `artists_vector_index` de MongoDB debe usar la
ruta `embedding` y dimension 384.

### Migracion manual desde BGE

Los vectores BGE no son compatibles con OpenAI, aunque ambos tengan 384
dimensiones. Antes de habilitar la nueva version, detene las importaciones y
busquedas de artistas y realiza un respaldo de la coleccion. Elimina manualmente
el campo `embedding` de todos los documentos de `artists`; no borres los demas
campos. Despliega la nueva version con `OPENAI_API_KEY` configurada. Al iniciar,
regenerara los vectores faltantes con OpenAI. Comprueba en los logs que la
regeneracion termino sin errores, que no quedan artistas sin embedding y que
el indice vectorial termino de actualizarse antes de reanudar el trafico.
Si falla la regeneracion, corrige el error y reinicia: solo se procesaran los
vectores que sigan faltando. Restaurar los vectores BGE requiere volver tambien
a la version anterior de la aplicacion.