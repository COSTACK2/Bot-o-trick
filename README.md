# DPI Trick

Utilitário Android com uma barra flutuante (overlay) que permite alternar
rapidamente o DPI (densidade de tela) do sistema entre um valor "normal" e
um valor "alto" configuráveis pelo usuário.

## O que o app faz

- Ao abrir, pede a permissão de **sobreposição** (aparecer sobre outros apps).
- Depois de concedida, inicia um serviço em primeiro plano que desenha uma
  barra discreta no topo da tela com:
  - Campos para digitar o **DPI alto** e o **DPI normal**;
  - Um botão **Trick**, que liga/desliga a troca de DPI;
  - Um botão para **minimizar** a barra (ela vira um pequeno botão circular
    roxo "T" que pode ser **arrastado** livremente pela tela — tocar nele
    de novo reabre a barra completa);
  - Um botão para **fechar** a barra (se o Trick estiver ativo, o DPI normal
    é restaurado automaticamente antes de fechar).
- A barra continua funcionando mesmo com o app em segundo plano, graças a um
  `Foreground Service` com notificação de prioridade mínima.

## Estrutura do projeto

```
DPITrick/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/dpitrick/
        │   ├── MainActivity.kt      -> tela inicial, pede permissões
        │   ├── OverlayService.kt    -> desenha e controla a barra flutuante
        │   ├── DpiUtils.kt          -> lê/altera o DPI do sistema
        │   └── PrefsManager.kt      -> salva os valores de DPI escolhidos
        └── res/
            ├── layout/
            │   ├── activity_main.xml
            │   └── overlay_bar.xml
            ├── values/ (strings.xml, colors.xml, themes.xml)
            └── drawable/ (ícones e formas vetoriais, sem imagens binárias)
```

## Como importar no Android Studio

1. Baixe/clone este repositório.
2. Abra o **Android Studio** → `File > Open...` → selecione a pasta `DPITrick`.
3. Aguarde a sincronização do Gradle (o Android Studio baixa automaticamente
   o Gradle e as dependências necessárias na primeira vez).
4. Conecte um dispositivo Android (ou use um emulador) e clique em **Run ▶**.

> Se preferir usar a linha de comando, rode `gradle wrapper` uma vez (com o
> Gradle instalado na sua máquina) para gerar os arquivos `gradlew`/`gradlew.bat`,
> e depois use `./gradlew assembleDebug` normalmente (veja a seção abaixo).

## Permissões necessárias

### 1) Sobreposição / aparecer sobre outros apps (obrigatória)
Concedida direto pelo app: toque em **"Conceder permissão"** na tela inicial,
o Android abre a tela de configuração e você ativa a chave para o app.

### 2) Alterar o DPI do sistema (necessária para o botão Trick funcionar)
Alterar a densidade de tela é uma operação **protegida** pelo Android — não
existe um botão de "conceder" para isso na interface do sistema. O app tenta
dois caminhos, e você precisa garantir **pelo menos um** deles:

**Opção A — Conceder `WRITE_SECURE_SETTINGS` via ADB (recomendado, sem root)**

Com o dispositivo conectado ao computador (depuração USB ativada), rode:

```bash
adb shell pm grant com.example.dpitrick android.permission.WRITE_SECURE_SETTINGS
```

Esse comando só precisa ser executado uma vez (a permissão persiste até você
desinstalar o app ou revogá-la manualmente).

**Opção B — Dispositivo com root**

Se o aparelho tiver acesso root (binário `su`), o app tenta automaticamente
executar `wm density <valor>` via `su -c`, sem precisar da opção A.

Sem nenhuma das duas opções, o botão Trick mostra um aviso informando que a
alteração de DPI não pôde ser aplicada.

### 3) Notificações (Android 13+)
Necessária para exibir a notificação de serviço em primeiro plano, exigida
pelo próprio Android para manter a barra ativa em segundo plano. O app pede
essa permissão junto com a de sobreposição.

## Como gerar o APK

**Pelo Android Studio:**
`Build > Build Bundle(s) / APK(s) > Build APK(s)`
O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.

**Pela linha de comando:**
```bash
./gradlew assembleDebug
```
ou, para uma versão de release (não assinada):
```bash
./gradlew assembleRelease
```

## Observações técnicas

- Escrito em **Kotlin**, usando `WindowManager` + `TYPE_APPLICATION_OVERLAY`
  (com fallback para `TYPE_PHONE` em versões antigas do Android) para o overlay.
- `minSdk 24`, `targetSdk`/`compileSdk 34`.
- Os valores de DPI e o estado do Trick ficam salvos em `SharedPreferences`,
  então persistem entre reinícios da barra.
- Este projeto não contém nenhuma funcionalidade voltada a jogos — é apenas
  um utilitário genérico de troca rápida de DPI.
