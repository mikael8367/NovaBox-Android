# NovaBox Android

Launcher experimental de ambientes de compatibilidade para Android, com interface própria em Kotlin + Jetpack Compose.

## Interface

- Tema escuro próprio com detalhes verde-neon.
- Navegação inferior animada: Início, Ambientes, Motores e Ajustes.
- Biblioteca de perfis com editar, excluir e salvar configurações.
- Prévia de desktop com cursor de mouse controlado pelo toque.
- Interface responsiva e sem imagens ou telas copiadas de outros launchers.

## Perfis de sistemas

O catálogo oferece perfis de Windows (95 até 11), distribuições Linux, BSD e outros sistemas, como FreeDOS, ReactOS, Haiku e Android-x86.

**Importante:** selecionar um perfil não baixa nem instala o sistema operacional. Os perfis descrevem o tipo de ambiente e a cadeia de compatibilidade necessária.

## Configurações por ambiente

- Família e versão do sistema.
- Arquitetura do programa.
- CPU e memória configuráveis.
- Armazenamento virtual.
- Resolução, DPI, limite de FPS e memória gráfica virtual.
- Backend gráfico.
- Áudio, rede e modo de mouse.
- Tela cheia e desktop virtual.
- Opções Wine, DXVK, VKD3D-Proton, ESYNC, FSYNC e WoW64.
- Perfil Box64, idioma, variáveis de ambiente e caminho de pasta compartilhada.

Os dados dos ambientes são persistidos no armazenamento privado do aplicativo.

## Motores e limitações atuais

O catálogo reconhece Wine, Box64, Box86, PRoot, DOSBox, QEMU, Mesa, VirGL, Zink, Turnip, DXVK e VKD3D-Proton.

A integração dos binários nativos ainda não está concluída. O app verifica a presença dos executáveis e mostra os componentes ausentes; ele não declara um runtime instalado sem encontrá-lo.

- Wine + Box64/Box86: caminho planejado para aplicativos Windows.
- PRoot: caminho planejado para ambientes Linux de usuário.
- DOSBox: caminho planejado para FreeDOS e aplicativos DOS.
- QEMU: necessário para sistemas completos como BSD, ReactOS e Haiku; ainda não integrado.

A prévia de desktop e o mouse atual são demonstrações visuais. O cursor ainda não envia eventos para uma sessão Wine/Linux real.

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
