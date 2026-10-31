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

## Motores e limitações atuais

O catálogo reconhece e gerencia Wine (compatibilidade Windows), Box64, Box86, PRoot, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip, DXVK e VKD3D-Proton.

O gerenciador agora possui um registro técnico por componente, com arquivos esperados, dependências, ABI mínima e requisitos gráficos. A tela Motores valida arquivos reais no diretório privado do app, tamanho não zero e permissão de execução para binários. Os estados distinguem instalado, ausente, incompleto e incompatível com o aparelho. Marcadores vazios não são aceitos como instalação. A verificação de Turnip exige indícios de GPU Qualcomm Adreno e Vulkan; DXVK, VKD3D-Proton e Zink verificam o suporte Vulkan anunciado pelo Android.

**Limitação atual:** os pacotes nativos e seus instaladores ainda não estão publicados/integrados. Portanto, esta etapa entrega o gerenciador e a validação real dos componentes, mas não baixa nem instala os motores e ainda não executa Windows/Linux/DOS ou máquinas QEMU. Não considere a prévia visual como um sistema iniciado. Os pacotes seguintes precisam vir de fontes upstream confiáveis, com ABI correta, hashes e licenças verificadas.

- Wine + Box64/Box86: caminho planejado para aplicativos Windows.
- PRoot: caminho planejado para ambientes Linux de usuário.
- DOSBox: caminho planejado para FreeDOS e aplicativos DOS.
- QEMU: necessário para Android virtual, BSD, ReactOS e Haiku; ainda não integrado. Para Android convidado, também será necessário configurar a rede user-mode/NAT do QEMU. Os perfis Android são configurações de máquina convidada, não uma segunda instalação Android funcionando ainda.

A prévia de desktop e o mouse atual são demonstrações visuais. O cursor ainda não envia eventos para uma sessão Wine/Linux real. Os perfis Android convidados também precisam de uma imagem de sistema e de um backend QEMU integrado antes de inicializar.

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
