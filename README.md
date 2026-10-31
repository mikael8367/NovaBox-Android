# NovaBox Android

Launcher experimental de ambientes de compatibilidade para Android, com interface própria em Kotlin + Jetpack Compose.

## Interface

- Tema escuro próprio com detalhes verde-neon.
- Navegação inferior animada: Início, Ambientes, Motores, Programas e Ajustes.
- Biblioteca de perfis com editar, excluir e salvar configurações.
- Prévia de desktop com cursor de mouse controlado pelo toque.
- Interface responsiva e sem imagens ou telas copiadas de outros launchers.

## Perfis de sistemas

O catálogo oferece perfis de Windows (3.1, 95, 98, ME, NT 4.0, 2000, XP, Vista, 7, 8, 8.1, 10 e 11), distribuições Linux (Ubuntu, Debian, Mint, Fedora, Arch, Manjaro, Alpine, openSUSE, Kali, Raspberry Pi OS e outras), BSD (FreeBSD, OpenBSD, NetBSD e DragonFly BSD), Android virtual (Android 4.4 até 16, Android Go 8–16, AOSP 14–16, Android TV 9–16, Android Automotive 12–16, Wear OS 3–6, Android-x86 32/64-bit, Bliss OS, PrimeOS e ROMs como LineageOS, GrapheneOS, CalyxOS, /e/OS, crDroid, Evolution X, PixelOS, ArrowOS, OmniROM, Paranoid Android, Resurrection Remix, Replicant, iodéOS e DivestOS legado) e outros como FreeDOS, ReactOS, Haiku, Solaris/illumos, OS/2 e BeOS. macOS e ChromeOS aparecem apenas como não suportados.

**Importante:** selecionar um perfil não baixa nem instala o sistema operacional. O catálogo inclui mais de 50 perfis Android, mas cada um precisa de uma imagem compatível. ROMs como GrapheneOS, LineageOS e outras dependem de builds para dispositivos/arquiteturas específicos e não são garantidas em uma VM genérica.

## Configurações por ambiente

- Família e versão do sistema. O catálogo Android possui pesquisa por nome, versão e descrição.
- Arquitetura do programa.
- CPU, memória e perfil Box64 configuráveis.
- Armazenamento virtual.
- Resolução, DPI, limite de FPS e memória gráfica virtual (parâmetro de perfil, não reserva de VRAM física).
- Backend gráfico.
- Áudio, Internet compartilhada (NAT) ou rede desativada e modo de mouse.
- Tela cheia e desktop virtual.
- Opções Wine prefix configuration: the launcher now explicitly creates 64-bit prefixes (WINEARCH=win64), matching the bundled x86-64 Wine build. The per-environment Esync/Fsync toggles are passed to Wine only when enabled. This does not add a graphical display backend or guarantee that Fsync is supported by the Android kernel.

Wine, DXVK, VKD3D-Proton, ESYNC, FSYNC e WoW64.
- Perfil Box64, idioma, variáveis de ambiente e caminho de pasta compartilhada.
- Perfil de desempenho (economia/equilibrado/alto desempenho), orientação, tela ligada, contador de FPS, teclado virtual, aceleração gráfica, captura do mouse, área de transferência e compartilhamento de arquivos.
- Windows/Wine: modo de jogo, ajustes de registro e serviços como opções de perfil, DPI alto, argumentos de inicialização e substituições de DLL.
- Android convidado: animações, janelas múltiplas, Google Play Services (se a imagem tiver os pacotes), notificações, apps em segundo plano, depuração ADB e animação de boot.

As opções novas são salvas por ambiente e ambientes antigos recebem valores padrão ao serem carregados. Enquanto Wine/QEMU e os demais motores não consumirem esses parâmetros, elas representam configurações desejadas do perfil — não alterações já aplicadas ao sistema convidado.

Os dados dos ambientes são persistidos no armazenamento privado do aplicativo. Windows/Wine está configurado para compartilhar a rede do Android hospedeiro quando o runtime estiver ativo. Android virtual usa o modo NAT como configuração desejada, mas precisa do encaminhamento de rede do QEMU para funcionar.

## Workspaces Wine (preparação inicial)

Na aba **Programas**, cada perfil Windows/Wine pode criar uma estrutura privada e isolada em `filesDir/wine-workspaces/<environment-id>`. O gerenciador valida o perfil e o identificador, impede saída do diretório raiz por path traversal, cria pastas separadas para prefix, home, temporários, logs e arquivos compartilhados e grava metadados versionados por substituição temporária.

O estado inicial é registrado como `WORKSPACE_ONLY` e `wineBootCompleted=false`. Depois que o bundle Wine/Box64 estiver instalado, a ação **Inicializar prefixo Wine** executa `wineboot --init`; somente quando o processo termina com código zero os metadados passam para `WINE_INITIALIZED`. A criação preserva um prefixo já inicializado, e a exclusão do ambiente também remove sua estrutura privada; se a limpeza falhar, o perfil é preservado.

A inicialização do prefixo e a preparação do processo estão implementadas. Foi adicionada uma ponte opcional para detectar e abrir o aplicativo Termux:X11 (`com.termux.x11`), com consulta de visibilidade de pacote no manifesto. O usuário precisa instalar e iniciar o servidor X11 separadamente; abrir o aplicativo não garante que o display `:0` esteja ativo nem que Wine consiga desenhar nele. Ainda faltam a ligação do backend gráfico ao processo Wine, encaminhamento real de mouse/teclado, áudio e ciclo de vida de processos, seguida de testes em aparelho.

## Biblioteca de programas e diagnóstico

A aba **Programas** permite importar arquivos `.exe`, `.msi` e `.com` para um ambiente. O app copia o conteúdo para o armazenamento privado, limita cada arquivo a 512 MiB, confere o tamanho informado pelo provedor e calcula SHA-256. Os arquivos podem ser removidos pela própria biblioteca. Cada item também oferece uma verificação manual de integridade: o NovaBox relê o conteúdo em fluxo, recalcula SHA-256 e compara com o hash registrado na importação. A leitura ocorre em segundo plano para não bloquear a interface. Nenhuma permissão ampla de armazenamento é solicitada.

O cartão de cada ambiente Wine também oferece **Abrir log do Wine**. O visualizador lê apenas o final do arquivo, limitado a 32 KiB, valida o identificador e confirma que o caminho permanece dentro da pasta privada do ambiente. Enquanto a aba Programas está aberta, o estado dos processos é atualizado periodicamente para refletir encerramentos externos ou inesperados. Esses recursos melhoram o diagnóstico, mas não substituem o backend gráfico ainda ausente.

O bundle Wine/Box64 e a camada de preparação do prefixo estão disponíveis. A aba Motores agora inclui um cartão de integração opcional com Termux:X11 para verificar e abrir o app externo. A biblioteca ainda não oferece execução gráfica funcional por botão: o XServer não é embutido nem iniciado automaticamente, e faltam o canal gráfico conectado ao Wine, integração real de entrada e áudio, clipboard e ciclo de vida completo da sessão. O cartão de diagnóstico nas configurações mostra modelo/Android, ABI, RAM, espaço livre, OpenGL ES e presença da feature Vulkan; o relatório pode ser copiado para suporte.

## Motores, instaladores e limitações atuais

O catálogo reconhece Wine, Box64, Box86, PRoot, Alpine Linux ARM64, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip, DXVK e VKD3D-Proton. A aba **Motores** mostra o estado dos arquivos no armazenamento privado e distingue instalado, ausente, incompleto, incompatível e bloqueado por dependências. Para binários executáveis nativos, a validação também lê o cabeçalho ELF e confere classe, endianness e arquitetura (ARM64, ARM32, x86 ou x86-64) com a ABI permitida pelo componente. Assim, um arquivo x86-64 não é aceito como executável ARM64 apenas por ter nome e permissão corretos. Essa checagem não prova compatibilidade com o linker/Bionic, dependências dinâmicas, SELinux ou capacidade de iniciar uma sessão.

### Instalador verificado disponível

- **DXVK 3.1.1**: pacote da [release oficial do projeto DXVK](https://github.com/doitsujin/dxvk/releases/tag/v3.1.1), baixado por HTTPS. A licença zlib/libpng é preservada quando presente no arquivo.
- O instalador exige o tamanho publicado (18.041.512 bytes) e o SHA-256 fixado no catálogo.
- O download mostra bytes reais recebidos e pode ser cancelado.
- O extrator TAR.GZ rejeita caminhos absolutos, `..`, links e tipos de entrada não permitidos. Só extrai as DLLs listadas no manifesto, limita a expansão e valida todos os arquivos necessários.
- Os arquivos são preparados em uma pasta temporária. A pasta anterior é preservada até a nova preparação passar pelas validações e ser movida para o destino.
- Um arquivo incompleto, com hash incorreto ou com estrutura diferente é descartado; ele nunca é marcado como instalado.

**Limitações do DXVK:** o pacote apenas armazena as DLLs no diretório privado do motor; ainda não as registra automaticamente em um prefixo Wine, não instala Wine, não fornece Vulkan e não inicia jogos sozinho. O upstream não declara suporte oficial ao Android, portanto a integração é experimental e depende de uma pilha Vulkan funcional e de Wine compatível. O aparelho também pode ser considerado incompatível pela verificação de recursos do Android.

### Bundle Wine + Box64 disponível

- **Wine + Box64 0.4.4**: bundle baseado nos artefatos publicados pelo projeto Winlator, com rootfs Linux/glibc, patches, Box64 ARM64 e PulseAudio.
- Os arquivos são fixados no commit upstream `8077f374ef2d54127b088de9e8cd7b4af14d9c8b` e verificados por tamanho e Git blob SHA-1 antes da extração.
- O instalador suporta TAR+Zstandard, rejeita traversal, links que escapem do rootfs, dispositivos e FIFOs, limita a expansão e prepara tudo em staging antes da troca atômica.
- A instalação exige aproximadamente 1,7 GB livres. Os avisos e licenças de terceiros estão em `THIRD_PARTY_NOTICES.md`.

O bundle fornece a base real de Wine/Box64 e inicialização de prefixo; não significa que a sessão Windows gráfica esteja pronta. O backend XServer/display, entrada e áudio ainda não estão integrados.

### Motores ainda sem instalador

O instalador de **Alpine Linux 3.24.3 ARM64** baixa o minirootfs oficial, confere tamanho e SHA-256 fixados, extrai em staging com proteção contra path traversal e symlinks externos e ativa por troca atômica. O instalador de **PRoot Android ARM64** baixa um pacote de build upstream fixado em commit imutável, verifica tamanho e Git blob SHA-1, extrai apenas arquivos regulares em staging e testa o executável com `--version` antes de ativar. A aba Motores oferece os dois instaladores e um terminal de comandos Alpine que só habilita a execução quando o rootfs e o PRoot estão presentes. A inicialização ainda precisa ser validada em aparelho real. Box86, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip e VKD3D-Proton continuam sem instaladores completos e verificados neste projeto. O gerenciador QEMU agora prepara discos RAW esparsos, valida kernel/initrd, limita RAM pelo estado de memória do Android e monta uma linha de comando ARM64 com rede NAT opcional e log serial. Ainda não há binário QEMU Android/ARM64 instalável, tela/VNC nem firmware/imagem Android convidada; portanto a VM não pode ser iniciada no app ainda. Pacotes Linux x86-64 não são tratados como binários Android instaláveis.

Turnip é um driver Vulkan para GPUs Qualcomm Adreno. Ele não é adequado para GPUs PowerVR como a do Samsung Galaxy A12. DXVK, VKD3D-Proton e Zink também exigem Vulkan; a presença do componente no catálogo não garante compatibilidade com o aparelho.

A seleção de perfil não baixa nem instala um sistema operacional automaticamente. O app oferece a instalação opcional do bundle Wine/Box64 e do rootfs Alpine ARM64; o primeiro ainda não tem sessão gráfica e o segundo ainda não inicializa sem PRoot. DOS e Android virtual continuam sem motores integrados. A prévia visual de desktop continua sendo uma demonstração, não uma máquina virtual em execução.

## Compilação e validação

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- JDK 17
- Android SDK 35
- Gradle 8.9

O GitHub Actions executa compilação Kotlin e testes unitários do catálogo e das configurações.

Nenhum APK é gerado automaticamente.

## Pacote Android

`com.mikael.novabox`


### Experimental Wine process launch

Imported EXE/MSI/COM entries now have an explicit process-launch action wired to the Box64/Wine launcher, with per-environment stop/status handling and output appended to `wine-workspaces/<environment-id>/logs/wine-session.log`. This is process-level execution only. The app does not yet provide an integrated X11/Wayland display, audio, or input backend, so GUI applications are not expected to show a window. The Wine environment intentionally no longer hardcodes `DISPLAY=:0`; a display must be provided by a real, integrated backend before graphical execution can be claimed.


### Inicialização do Termux:X11 pelo NovaBox

O cartão de servidor gráfico pode solicitar o início do display `:1` através do serviço oficial `com.termux.app.RunCommandService` do Termux e abrir o aplicativo Termux:X11. Para isso, Termux e Termux:X11 devem estar instalados, o usuário deve conceder a permissão Android `com.termux.permission.RUN_COMMAND` ao NovaBox e habilitar `allow-external-apps=true` em `~/.termux/termux.properties` no Termux. O app não altera essa configuração automaticamente.

O resultado `START_REQUESTED` significa apenas que o serviço aceitou o pedido e a Activity externa foi aberta; não é uma confirmação de que o servidor está pronto. Esta etapa ainda não encaminha o socket X11 para o processo Wine isolado no sandbox do NovaBox. O transporte gráfico, autenticação X11 e integração de entrada/áudio continuam pendentes e não são anunciados como funcionais.

### Encerramento de sessões Wine

- Ao encerrar um ambiente, o NovaBox identifica os processos descendentes do processo Box64/Wine pelo arquivo de filhos em `/proc`.
- Os auxiliares descendentes são encerrados antes do processo raiz, reduzindo o risco de Wine, serviços e instaladores ficarem órfãos em segundo plano.
- A descoberta ignora PID inválido e duplicatas. O comportamento ainda precisa ser validado em aparelho Android real, pois as restrições de leitura de `/proc` variam por versão e fabricante.
