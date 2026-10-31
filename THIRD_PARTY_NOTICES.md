# Avisos de terceiros — NovaBox

## Runtime Wine/Box64

O bundle de runtime opcional usa arquivos publicados no repositório **Winlator**:

- Projeto: https://github.com/brunodev85/winlator-app
- Commit imutável utilizado: `8077f374ef2d54127b088de9e8cd7b4af14d9c8b`
- Código-fonte do Winlator: GNU Lesser General Public License 2.1 (LGPL-2.1).
- Créditos upstream incluem Wine, Box86/Box64, Mesa, DXVK, VKD3D e CNC DDraw.

Os arquivos binários de runtime são baixados separadamente, sob demanda, e validados pelo tamanho e pelo identificador Git blob publicado no commit fixado. Os avisos/licenças presentes nos arquivos extraídos devem ser preservados. As licenças dos componentes de terceiros continuam sendo as de seus respectivos projetos; este aviso não substitui os textos de licença distribuídos por cada componente.

O NovaBox é um projeto independente e não é afiliado oficialmente ao Winlator.


## Alpine Linux ARM64 rootfs

- Projeto: https://alpinelinux.org/
- Artefato: Alpine Linux minirootfs 3.24.3 para aarch64.
- Origem: https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/alpine-minirootfs-3.24.3-aarch64.tar.gz
- SHA-256 publicado pelo projeto: `4d3f80afaebd447c5591a63a73bde380a149fe8b2d85488cafabb2ff97551f50`.
- O instalador preserva a estrutura e os metadados de licença distribuídos dentro do rootfs. Os pacotes incluídos mantêm as licenças dos respectivos projetos upstream.


## PRoot Android ARM64

- Projeto de empacotamento: https://github.com/green-green-avk/build-proot-android
- Commit imutável: `01f83b8841358450c78333d1b33ab30d4943bec4`.
- Pacote: `packages/proot-android-aarch64.tar.gz`, tamanho 179.448 bytes.
- Integridade: Git blob SHA-1 `45158586e587e1adf9a75db687dfdb08baec83c5`, validado sobre os bytes baixados.
- Os scripts de empacotamento são MIT; o PRoot incluído é GPL-2.0. O pacote upstream contém loaders que são preservados junto ao executável. Consulte também os avisos de talloc distribuídos no pacote upstream.
