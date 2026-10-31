# NovaBox Android

Aplicativo Android experimental inspirado em launchers de compatibilidade Windows, com identidade visual própria.

## Estado atual

Este repositório começa como uma base nativa Android em Kotlin + Jetpack Compose.

- Interface escura com detalhes verde neon.
- Navegação inferior animada: Início, Containers, Downloads e Configurações.
- Estrutura inicial para gerenciamento de containers.
- Nenhum APK é gerado automaticamente.

## Importante

A interface inicial não executa programas Windows ainda. A execução real depende da integração e validação de componentes nativos compatíveis com a arquitetura e a versão do Android. O projeto não deve apresentar recursos de Wine/Box64 como funcionais até que estejam integrados e testados.

## Requisitos de desenvolvimento

- Android Studio recente
- JDK 17
- Android SDK 35
- Gradle 8.9 ou compatível

## Compilar

Abra o projeto no Android Studio e execute a configuração `app`.

## Pacote

`com.mikael.novabox`
