# 📺 PelisJuanita TV Bridge

Aplicación nativa para Android TV y dispositivos Android diseñada para ofrecer una experiencia optimizada de navegación y reproducción multimedia mediante control remoto.

## 🚀 Descripción

PelisJuanita TV Bridge transforma una experiencia web tradicional en una aplicación nativa adaptada para televisores y dispositivos sin pantalla táctil. El proyecto fue desarrollado con foco en rendimiento, compatibilidad y facilidad de uso, permitiendo navegar y reproducir contenido multimedia utilizando únicamente un control remoto.

La aplicación implementa un sistema de cursor virtual avanzado, soporte para reproducción en pantalla completa y optimizaciones específicas para Smart TVs basadas en Android.

-RECOMIENDO AMPLIAMENTE LEER MIS COMENTARIOS DE AUTOR-

---

## ✨ Características

### 🎯 Cursor Virtual Inteligente

- Navegación mediante D-Pad (control remoto).
- Cursor visual controlado por las flechas del mando.
- Movimiento continuo con aceleración progresiva.
- Auto-scroll horizontal y vertical al alcanzar los bordes de la pantalla.
- Simulación de eventos táctiles nativos para garantizar compatibilidad con interfaces web y reproductores multimedia.
- Configuracion personalizada de la web y la velocidad del cursor en opciones (El control remoto debe tener dicho boton)

### 🎬 Experiencia Multimedia Optimizada

- Reproducción de video en pantalla completa real.
- Integración con `WebChromeClient` para manejo avanzado de contenido multimedia.
- Aparición automática de controles de reproducción al interactuar con el video.
- Ocultación automática de controles para una experiencia más inmersiva.
- Gestión prioritaria del control remoto durante la reproducción.

### ⚡ Compatibilidad y Conectividad

- Compatibilidad con Android TV y dispositivos Android móviles.
- Gestión optimizada de cookies y almacenamiento web.
- Configuración avanzada de WebView para maximizar compatibilidad con sitios modernos.
- Identificación como navegador móvil compatible para mejorar la experiencia de navegación.

### 🖥️ Optimización para Smart TV

- Aceleración por hardware habilitada.
- Renderizado optimizado para contenido HD y 4K.
- Interfaz limpia y minimalista.
- Sin publicidad integrada.
- Soporte para banner Leanback de Android TV.

---

## 🏗️ Arquitectura

### MainActivity

Responsable de:

- Gestión del ciclo de vida de la aplicación.
- Interceptación de eventos del control remoto.
- Comunicación entre la interfaz nativa y la capa web.
- Inyección y ejecución de scripts necesarios para la navegación.

### TvWebView

Componente personalizado basado en WebView encargado de:

- Renderizar contenido web.
- Gestionar eventos de navegación.
- Procesar interacciones del cursor virtual.
- Controlar la experiencia multimedia.

### Sistema de Intercepción de Entradas

Implementa una capa de captura de eventos que garantiza que las acciones del control remoto sean procesadas por la aplicación antes de llegar al contenido web.

---

## 📁 Estructura del Proyecto

```text
app/
├── java/
│   ├── MainActivity.java
│   └── TvWebView.java
│
├── res/
│   ├── layout/
│   │   └── activity_main.xml
│   ├── drawable/
│   └── mipmap/
│
├── AndroidManifest.xml
│
└── .gitignore
```

### Archivos Principales

| Archivo | Descripción |
|----------|------------|
| `MainActivity.java` | Lógica principal y gestión de eventos |
| `TvWebView.java` | Motor de navegación personalizado |
| `activity_main.xml` | Interfaz de usuario |
| `AndroidManifest.xml` | Configuración de la aplicación |
| `.gitignore` | Exclusiones para Git |

---

## 🛠️ Tecnologías Utilizadas

- Java
- Android SDK
- Android WebView
- WebChromeClient
- Android TV Leanback
- Android API 34

---

## 📋 Requisitos

- Android 8.0 (API 26) o superior
- Android TV compatible o dispositivo Android
- Conexión a Internet

---

## 🔧 Compilación

```bash
git clone https://github.com/usuario/pelisjuanita-tv-bridge.git

cd pelisjuanita-tv-bridge
```

Abrir el proyecto en Android Studio y ejecutar:

```bash
Build > Make Project
```

o

```bash
Run > Run App
```

---

## 📈 Estado del Proyecto

- ✅ Estable
- ✅ Optimizado para Android TV
- ✅ Compatible con control remoto
- ✅ Reproducción multimedia funcional
- ✅ Listo para despliegue y distribución

---

## 👨‍💻 Autor

**Mauro G. Martínez**

Este proyecto nació por una necesidad muy simple: mis padres utilizan habitualmente PelisJuanita, pero querían disfrutarla desde el televisor. Después de probar distintos navegadores en Smart TVs, me encontré con una realidad aplastante: la experiencia suele HORRIBLE, INCOMODA y poco amigable para usar con un control remoto.

Por eso decidí desarrollar este puente para Android TV, intentando llevar la experiencia lo más cerca posible de una aplicación nativa, con navegación optimizada, soporte para control remoto y reproducción multimedia en pantalla completa.

Quiero dejar en claro que este proyecto NO está afiliado a PelisJuanita ni reemplaza su trabajo. Mi aporte consiste únicamente en facilitar el acceso desde dispositivos Android TV, soy meramente un PUENTE. Todo el mérito por la plataforma y el enorme trabajo detrás de ella corresponde a sus creadores.

* Nota sobre la conectividad *

En algunas regiones o proveedores de Internet, el acceso a PelisJuanita puede presentar inconvenientes de resolución DNS. Personalmente, he obtenido mejores resultados utilizando los servidores DNS públicos de Google

Sin embargo, esto puede variar según el país, el proveedor o las condiciones de red de cada usuario.

* UN DATO MAS! ✅🚀
  
Si tienen problemas con CloudFlare, deben dejar que cargue y luego tocar el boton (o esperar) para continuar y que el detecte que no son un bot, dependiendo de su televisor, puede tardar poco o mucho.

Espero que este proyecto pueda ser útil para otras personas que, como yo, simplemente buscaban una mejor experiencia para disfrutar contenido desde el televisor.

¡Un abrazo y gracias por probar la aplicación! 📺🚀

---

## 📜 Licencia

Este proyecto se distribuye únicamente con fines educativos y de investigación. El contenido reproducido pertenece a sus respectivos propietarios.
