# NovaBox Android

Launcher experimental de ambientes de compatibilidade para Android, com interface própria em Kotlin + Jetpack Compose.

## Interface

- Tema escuro próprio com detalhes verde-neon.
- Navegação inferior animada: Início, Ambientes, Motores, Programas e Ajustes.
- Biblioteca de perfis com editar, excluir e salvar configurações.
- Prévia de desktop com cursor de mouse controlado pelo toque.
- Interface responsiva e sem imagens ou telas copiadas de outros launchers.

## Motores e runtime Wine/Box64

O NovaBox agora contém um instalador de bundle real baseado nos artefatos publicados pelo projeto Winlator, fixados no commit `8077f374ef2d54127b088de9e8cd7b4af14d9c8b`:

- Root filesystem Linux/glibc do Winlator.
- Patches do rootfs.
- Box64 0.4.4 para host ARM64.
- Pacote de áudio PulseAudio do Winlator.
- Verificação de tamanho e Git blob SHA-1 de cada artefato, calculado no formato de objeto Git.
- Extração TAR+Zstandard com bloqueio de path traversal, links que escapem do rootfs, dispositivos, FIFOs e expansão excessiva.
- Instalação em staging e troca atômica do rootfs, preservando a versão anterior em caso de falha.
- Inicialização do Wine por Box64 usando o layout real `/usr/local/bin/box64` e `/opt/wine/bin/wine`.

A primeira instalação exige aproximadamente 1,7 GB livres e baixa os artefatos sob demanda. Os arquivos são provenientes do Winlator; veja `THIRD_PARTY_NOTICES.md` e preserve os avisos de licença que acompanham os componentes.

**Limitação importante:** este bundle fornece a base real Wine/Box64 e permite preparar o prefixo/lançar processos, mas a sessão gráfica completa ainda depende da integração do XServer Android e do backend de entrada/áudio. Não se declara compatibilidade gráfica pronta. O Galaxy A12 usa GPU PowerVR; drivers Turnip/Adreno não são compatíveis com ele.

## Linux, Android virtual e outros sistemas

Os perfis Linux e Android virtual continuam sendo alvos diferentes: Linux userland precisa de rootfs ARM64 + PRoot e Android virtual precisa de QEMU ARM64 + imagem de sistema convidado compatível. Um perfil salvo não equivale a um sistema instalado. Não são baixadas imagens de ROMs aleatórias ou builds de outros dispositivos como se fossem universais.

## Programas

A biblioteca importa EXE/MSI/COM para armazenamento privado, calcula SHA-256 e verifica integridade. A execução gráfica depende do backend de display/entrada.

## Compilação e validação

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- JDK 17
- Android SDK 35
- Gradle 8.9

GitHub Actions compila o Kotlin e executa testes unitários. Nenhum APK é gerado automaticamente.
