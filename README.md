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

El MCP `search_artists` usa el modelo local BGE-small-en cuantizado de
LangChain4j para generar vectores de 384 dimensiones. Al iniciar, la aplicacion
calcula y persiste los embeddings faltantes de la coleccion `artists`; los
artistas importados posteriormente se embeben al guardarse. El indice vectorial
`artists_vector_index` de MongoDB debe usar la ruta `embedding` y dimension 384.