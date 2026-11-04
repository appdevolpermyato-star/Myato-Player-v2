# 🎵 Myato Player — Reprodutor de Música Android

**Myato Player** é um reprodutor de música moderno, completo e profissional para Android, inspirado na experiência visual de aplicativos como Spotify, Apple Music e YouTube Music, com foco em reprodução offline, biblioteca local, equalizador avançado e facilidade de uso sem necessidade de login obrigatório.

---

## ✨ Principais Funcionalidades

### 1. 📂 Biblioteca Musical & Varredura Automática
- **Escaneamento Automático**: Identifica automaticamente arquivos de áudio no armazenamento do dispositivo (`FLAC`, `MP3`, `WAV`, `AAC`, `M4A`, `OGG`).
- **Pull-to-Refresh**: Mecanismo de puxar para atualizar que re-escaneia o armazenamento em busca de novas faixas adicionadas.
- **Busca em Tempo Real**: Pesquise instantaneamente por título da música, nome do artista ou álbum.
- **Ordenação Inteligente**: Ordene sua lista por Nome, Artista, Duração ou Faixas adicionadas recentemente.
- **Capas de Álbuns**: Carregamento assíncrono de capas com Coil.

### 2. 🎛️ Equalizador Profissional (Android Audio Effects API)
- **Controle de Graves (Bass Boost)**: Amplificação ajustável de 0 a 100%.
- **Virtualizador**: Efeito surround ajustável de 0 a 100%.
- **Equalizador Gráfico de 5 Bandas**: 60Hz, 230Hz, 910Hz, 3.6kHz e 14kHz com ajuste fino de -15dB a +15dB.
- **Presets de Áudio**: *Rock, Pop, Jazz, Clássico, Eletrônica, Hip-Hop, Flat e Personalizado*.

### 3. 🔔 Widget na Barra de Notificações & Tela de Bloqueio (Media3)
- **MediaNotificationManager**: Notificação de mídia do sistema com suporte a reprodução em segundo plano.
- **Controles de Reprodução**: Play, Pause, Anterior, Próxima e barra de progresso interativa (scrubbing) diretamente na gaveta de notificações e tela de bloqueio.
- **Metadados Completos**: Exibição da capa do álbum, nome da faixa e artista em tempo real.

### 4. 📑 Sistema Completo de Playlists
- Crie playlists ilimitadas com nomes e descrições personalizadas.
- Adição e remoção individual ou em lote de faixas.
- Reprodução completa com modos **Ordem Normal** e **Aleatório (Shuffle)**.
- Contador de músicas e cálculo da duração total da playlist em minutos.

### 5. ❤️ Músicas Favoritas
- Botão de coração com animação instantânea em cada faixa e no player em tela cheia.
- Filtro e busca de favoritos.
- Botão para tocar todos os favoritos em modo aleatório.

### 6. 📥 Importador de Músicas & Downloads
- Seletor de arquivos de sistema (`Storage Access Framework`) para importar faixas de áudio diretamente da pasta Downloads ou cartão SD.
- Suporte a formatos de alta fidelidade sem perda de qualidade (`FLAC`).

### 7. 🤖 Smart Picks com IA Gemini
- Integração com **Google Gemini** (via Firebase AI Logic) para sugerir novos artistas e faixas com base nas músicas que você mais favoritou.

### 8. 🔒 Modo Offline & Sincronização Opcional
- **100% Funcional Offline**: Não exige login nem conta para tocar suas músicas locais.
- **Backup em Nuvem Opcional**: Botão de perfil para realizar Google Sign-In via Jetpack Credential Manager e sincronizar playlists com Firebase Firestore.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Kotlin 100%
- **Interface**: Jetpack Compose com Material Design 3 (M3)
- **Áudio & Background**: AndroidX Media3 (ExoPlayer + MediaSessionService)
- **Efeitos de Áudio**: Android AudioFX (`Equalizer`, `BassBoost`, `Virtualizer`)
- **Banco de Dados Local**: Room Database + SQLite
- **Arquitetura**: MVVM (Model-View-ViewModel) + Coroutines & StateFlow
- **Permissões**: `PermissionHelper` para Android 13+ (`READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`) e Android 12 ou inferior (`READ_EXTERNAL_STORAGE`)
- **Backend & IA**: Firebase Auth, Cloud Firestore e Firebase AI Logic (Gemini 3.8 Flash)

---

## 🚀 Como Compilar e Gerar o APK

### Pré-requisitos
- Android Studio Ladybug ou superior
- JDK 17
- Android SDK com suporte a API 24 a 36

### Gerar APK Debug
```bash
gradle assembleDebug
```
O arquivo APK será gerado em:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📲 Como Instalar no Celular

1. Baixe o arquivo `app-debug.apk` ou `myato-player-v1.0.apk`.
2. No seu celular Android, abra o arquivo baixado.
3. Se solicitado, autorize a instalação de aplicativos de fontes desconhecidas para o navegador ou gerenciador de arquivos.
4. Conceda a permissão de acesso aos arquivos de áudio ao abrir o app para escanear suas músicas automaticamente.

---

## 🌐 Publicação no GitHub

Para publicar este projeto no GitHub a partir do AI Studio:
1. Abra o menu de opções / configurações no canto superior do AI Studio.
2. Selecione **"Push to GitHub"** (ou conecte sua conta do GitHub).
3. Escolha o nome do repositório (ex: `myato-player`) e confirme o push.
4. O código-fonte completo, recursos, ícones e este README serão enviados automaticamente para o seu repositório!

---

Desenvolvido para proporcionar a melhor experiência de reprodução musical no Android.
