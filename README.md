# NovaBox Android

Launcher experimental de ambientes de compatibilidade para Android, com interface própria em Kotlin + Jetpack Compose.

## Interface

- Tema escuro próprio com detalhes verde-neon.
- Navegação inferior animada: Início, Ambientes, Motores e Ajustes.
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
- Opções Wine, DXVK, VKD3D-Proton, ESYNC, FSYNC e WoW64.
- Perfil Box64, idioma, variáveis de ambiente e caminho de pasta compartilhada.
- Perfil de desempenho (economia/equilibrado/alto desempenho), orientação, tela ligada, contador de FPS, teclado virtual, aceleração gráfica, captura do mouse, área de transferência e compartilhamento de arquivos.
- Windows/Wine: modo de jogo, ajustes de registro e serviços como opções de perfil, DPI alto, argumentos de inicialização e substituições de DLL.
- Android convidado: animações, janelas múltiplas, Google Play Services (se a imagem tiver os pacotes), notificações, apps em segundo plano, depuração ADB e animação de boot.

As opções novas são salvas por ambiente e ambientes antigos recebem valores padrão ao serem carregados. Enquanto Wine/QEMU e os demais motores não consumirem esses parâmetros, elas representam configurações desejadas do perfil — não alterações já aplicadas ao sistema convidado.

Os dados dos ambientes são persistidos no armazenamento privado do aplicativo. Windows/Wine está configurado para compartilhar a rede do Android hospedeiro quando o runtime estiver ativo. Android virtual usa o modo NAT como configuração desejada, mas precisa do encaminhamento de rede do QEMU para funcionar.

## Motores, instaladores e limitações atuais

O catálogo reconhece Wine, Box64, Box86, PRoot, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip, DXVK e VKD3D-Proton. A aba **Motores** mostra o estado dos arquivos no armazenamento privado e distingue instalado, ausente, incompleto, incompatível e bloqueado por dependências.

### Instalador verificado disponível

- **DXVK 3.1.1**: pacote da [release oficial do projeto DXVK](https://github.com/doitsujin/dxvk/releases/tag/v3.1.1), baixado por HTTPS. A licença zlib/libpng é preservada quando presente no arquivo.
- O instalador exige o tamanho publicado (18.041.512 bytes) e o SHA-256 fixado no catálogo.
- O download mostra bytes reais recebidos e pode ser cancelado.
- O extrator TAR.GZ rejeita caminhos absolutos, `..`, links e tipos de entrada não permitidos. Só extrai as DLLs listadas no manifesto, limita a expansão e valida todos os arquivos necessários.
- Os arquivos são preparados em uma pasta temporária. A pasta anterior é preservada até a nova preparação passar pelas validações e ser movida para o destino.
- Um arquivo incompleto, com hash incorreto ou com estrutura diferente é descartado; ele nunca é marcado como instalado.

**Limitações do DXVK:** o pacote apenas armazena as DLLs no diretório privado do motor; ainda não as registra automaticamente em um prefixo Wine, não instala Wine, não fornece Vulkan e não inicia jogos sozinho. O upstream não declara suporte oficial ao Android, portanto a integração é experimental e depende de uma pilha Vulkan funcional e de Wine compatível. O aparelho também pode ser considerado incompatível pela verificação de recursos do Android.

### Motores ainda sem instalador

Wine, Box64, Box86, PRoot, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip e VKD3D-Proton continuam no catálogo, mas não recebem um botão de download até haver um pacote Android/ARM64 com arquitetura, dependências, licença, formato e hash verificados. Pacotes Linux x86-64, arquivos-fonte e pacotes de drivers de outras GPUs não são tratados como binários Android instaláveis.

Turnip é um driver Vulkan para GPUs Qualcomm Adreno. Ele não é adequado para GPUs PowerVR como a do Samsung Galaxy A12. DXVK, VKD3D-Proton e Zink também exigem Vulkan; a presença do componente no catálogo não garante compatibilidade com o aparelho.

A seleção de perfil não baixa nem instala um sistema operacional. Windows/Wine, Linux, DOS e sistemas QEMU só poderão ser inicializados quando seus motores, imagens e integrações estiverem disponíveis. A prévia visual de desktop continua sendo uma demonstração, não uma máquina virtual em execução.

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
