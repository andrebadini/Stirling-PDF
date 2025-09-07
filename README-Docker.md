# Guia de Configuração do Stirling-PDF com Docker

Este guia fornece instruções detalhadas para configurar e executar o Stirling-PDF usando Docker.

**Tags:** `docker`, `stirling-pdf`, `pdf`, `self-hosted`, `configuração`, `guia`

---

## Índice

- [Imagens Docker para Stirling-PDF](#imagens-docker-para-stirling-pdf)
  - [Versões Disponíveis](#versões-disponíveis)
- [Como Executar](#como-executar)
  - [Usando `docker run`](#usando-docker-run)
  - [Usando `docker-compose`](#usando-docker-compose)
- [Configuração](#configuração)
  - [Visão Geral](#visão-geral)
  - [Variáveis de Ambiente vs. `settings.yml`](#variáveis-de-ambiente-vs-settingsyml)
  - [Opções de Configuração (`settings.yml`)](#opções-de-configuração-settingsyml)
    - [`security`](#security)
    - [`premium`](#premium)
    - [`legal`](#legal)
    - [`system`](#system)
    - [`ui`](#ui)
    - [`endpoints`](#endpoints)
    - [`metrics`](#metrics)
    - [`processExecutor`](#processexecutor)
  - [Configurações Adicionais (`custom_settings.yml`)](#configurações-adicionais-custom_settingsyml)
- [Notas Adicionais](#notas-adicionais)
  - [Desabilitando Endpoints](#desabilitando-endpoints)
  - [Personalizando a Interface](#personalizando-a-interface)
  - [Parâmetros Apenas de Ambiente](#parâmetros-apenas-de-ambiente)
- [Exemplos de Variáveis de Ambiente](#exemplos-de-variáveis-de-ambiente)

---

## Imagens Docker para Stirling-PDF

As imagens Docker para o Stirling-PDF estão disponíveis no [Docker Hub](https://hub.docker.com/r/stirlingtools/stirling-pdf) sob `stirlingtools/stirling-pdf` e no [GitHub](https://github.com/Stirling-Tools/Stirling-PDF/pkgs/container/stirling-pdf) em `stirling-pdf`.

### Versões Disponíveis

O Stirling-PDF oferece três versões distintas, otimizadas para diferentes configurações de hardware. Para usuários com hardware de menor performance, recomendamos escolher uma das versões específicas. Para aqueles que desejam os recursos mais recentes, a tag `latest` é a recomendada.

| Versão       | Tag Recomendada     |
|--------------|---------------------|
| **Fat**      | `latest-fat`        |
| **Standard** | `latest`            |
| **Ultra Lite**| `latest-ultra-lite` |

---

## Como Executar

### Usando `docker run`

Execute o contêiner com o seguinte comando:

##### LINUX:

```bash
docker run -d \
  --name stirling-pdf \
  -p 8080:8080 \
  -v "./StirlingPDF/trainingData:/usr/share/tessdata" \
  -v "./StirlingPDF/extraConfigs:/configs" \
  -v "./StirlingPDF/customFiles:/customFiles/" \
  -v "./StirlingPDF/logs:/logs/" \
  -v "./StirlingPDF/pipeline:/pipeline/" \
  -e DISABLE_ADDITIONAL_FEATURES=true \
  -e LANGS=en_GB \
  docker.stirlingpdf.com/stirlingtools/stirling-pdf:latest
```


---
##### WINDOWS:

```BASH
docker run -d --name stirling-pdf -p 8080:8080 `
  -v "D:\dev\Stirling-PDF\StirlingPDF\trainingData:/usr/share/tessdata" `
  -v "D:\dev\Stirling-PDF\StirlingPDF\extraConfigs:/configs" `
  -v "D:\dev\Stirling-PDF\StirlingPDF\customFiles:/customFiles/" `
  -v "D:\dev\Stirling-PDF\StirlingPDF\logs:/logs/" `
  -v "D:\dev\Stirling-PDF\StirlingPDF\pipeline:/pipeline/" `
  -e DISABLE_ADDITIONAL_FEATURES=true `
  -e LANGS=en_GB `
  docker.stirlingpdf.com/stirlingtools/stirling-pdf:latest
```
### Usando `docker-compose`

Crie um arquivo `docker-compose.yml` com o seguinte conteúdo:

```yaml
version: '3.3'
services:
  stirling-pdf:
    image: docker.stirlingpdf.com/stirlingtools/stirling-pdf:latest
    ports:
      - '8080:8080'
    volumes:
      - ./StirlingPDF/trainingData:/usr/share/tessdata # Necessário para idiomas extras de OCR
      - ./StirlingPDF/extraConfigs:/configs
      - ./StirlingPDF/customFiles:/customFiles/
      - ./StirlingPDF/logs:/logs/
      - ./StirlingPDF/pipeline:/pipeline/
    environment:
      - DISABLE_ADDITIONAL_FEATURES=false
      - LANGS=en_GB
```

### Entendendo os Volumes e Caminhos de Arquivo

Os caminhos como `/configs` ou `/customFiles/` mencionados na documentação existem **dentro do contêiner Docker**. Você os acessa através das pastas locais que você mapeia usando a flag `-v` (volume).

Pense nisso como um espelho: a pasta no seu computador (o *host*) é o objeto real, e a pasta dentro do contêiner é o reflexo que a aplicação Stirling-PDF utiliza.

**Exemplo Prático (para o `settings.yml`):**

1.  **No seu computador**, crie a pasta `./StirlingPDF/extraConfigs/`.
2.  **Crie e salve seu arquivo `settings.yml`** dentro dessa pasta.
3.  Ao iniciar o contêiner, o Docker mapeia `./StirlingPDF/extraConfigs` para `/configs` dentro do contêiner. Assim, a aplicação encontrará seu arquivo em `/configs/settings.yml`.

| Finalidade                                    | Pasta no seu Computador (Host) | Pasta Correspondente no Contêiner |
|-----------------------------------------------|--------------------------------|-----------------------------------|
| **Configurações Gerais (`settings.yml`)**     | `./StirlingPDF/extraConfigs/`  | `/configs`                        |
| **Idiomas de OCR (`.traineddata`)**           | `./StirlingPDF/trainingData/`  | `/usr/share/tessdata`             |
| **Arquivos de UI Customizados (logo, etc.)**  | `./StirlingPDF/customFiles/`   | `/customFiles/`                   |
| **Logs da Aplicação**                         | `./StirlingPDF/logs/`          | `/logs/`                          |
| **Configurações de Pipeline**                 | `./StirlingPDF/pipeline/`      | `/pipeline/`                      |

---

## Configuração


### Visão Geral

O Stirling-PDF permite uma fácil personalização da aplicação, incluindo:

- Nome da aplicação customizado.
- Slogans, ícones, HTML, imagens e CSS customizados (via sobreposição de arquivos).

Existem duas maneiras de aplicar configurações:
1.  **Arquivo `settings.yml`**: Este arquivo é gerado no diretório `/configs` e segue o formato YAML padrão.
2.  **Variáveis de Ambiente**: Têm precedência sobre o arquivo `settings.yml`.

### Variáveis de Ambiente vs. `settings.yml`

Para configurar via variáveis de ambiente, converta a estrutura aninhada do YAML para maiúsculas, separadas por `_`.

**Exemplo em `settings.yml`:**
```yaml
security:
  enableLogin: 'true'
```

**Equivalente como variável de ambiente:**
```
SECURITY_ENABLELOGIN=true
```

### Opções de Configuração (`settings.yml`)

Abaixo está a lista de configurações disponíveis no arquivo `settings.yml`.

#### `security`
```yaml
security:
  enableLogin: false # mude para 'true' para habilitar o login
  csrfDisabled: false # mude para 'true' para desabilitar a proteção CSRF (não recomendado para produção)
  loginAttemptCount: 5 # bloqueia a conta do usuário após 5 tentativas; use -1 para desativar
  loginResetTimeMinutes: 120 # tempo de bloqueio da conta em minutos
  loginMethod: all # aceita 'all', 'normal' (apenas usuário/senha), 'oauth2' ou 'saml2'
  initialLogin:
    username: '' # nome de usuário inicial para o primeiro login
    password: '' # senha inicial para o primeiro login
  oauth2:
    enabled: false # 'true' para habilitar (enableLogin também deve ser 'true')
    client:
      keycloak:
        issuer: '' # URL do endpoint OpenID Connect Discovery do realm Keycloak
        clientId: ''
        clientSecret: ''
        scopes: openid, profile, email
        useAsUsername: preferred_username # campo a ser usado como nome de usuário [email | name | given_name | family_name | preferred_name]
      google:
        clientId: ''
        clientSecret: ''
        scopes: email, profile
        useAsUsername: email # [email | name | given_name | family_name]
      github:
        clientId: ''
        clientSecret: ''
        scopes: read:user
        useAsUsername: login # [email | login | name]
    issuer: '' # provedor que suporta OpenID Connect Discovery
    clientId: ''
    clientSecret: ''
    autoCreateUser: true # permite a criação automática de usuários não existentes
    blockRegistration: false # nega login SSO sem registro prévio por um admin
    useAsUsername: email # padrão é 'email'; campos customizados podem ser usados
    scopes: openid, profile, email
    provider: google # nome do seu provedor OAuth, ex: 'google' ou 'keycloak'
  saml2:
    enabled: false # apenas para clientes enterprise pagos
    provider: ''
    autoCreateUser: true
    blockRegistration: false
    registrationId: stirling
    idpMetadataUri: https://dev-XXXXXXXX.okta.com/app/externalKey/sso/saml/metadata
    idpSingleLoginUrl: https://dev-XXXXXXXX.okta.com/app/dev-XXXXXXXX_stirlingpdf_1/externalKey/sso/saml
    idpSingleLogoutUrl: https://dev-XXXXXXXX.okta.com/app/dev-XXXXXXXX_stirlingpdf_1/externalKey/slo/saml
    idpIssuer: ''
    idpCert: classpath:okta.cert
    privateKey: classpath:saml-private-key.key
    spCert: classpath:saml-public-cert.crt
```

#### `premium`
```yaml
premium:
  key: 00000000-0000-0000-0000-000000000000
  enabled: false # habilita a verificação de chave de licença para recursos pro/enterprise
  proFeatures:
    SSOAutoLogin: false
    CustomMetadata:
      autoUpdateMetadata: false
      author: username
      creator: Stirling-PDF
      producer: Stirling-PDF
    googleDrive:
      enabled: false
      clientId: ''
      apiKey: ''
      appId: ''
```

#### `legal`
```yaml
legal:
  termsAndConditions: https://www.stirlingpdf.com/terms-and-conditions
  privacyPolicy: https://www.stirlingpdf.com/privacy-policy
  accessibilityStatement: ''
  cookiePolicy: ''
  impressum: ''
```

#### `system`
```yaml
system:
  defaultLocale: en-US # define o idioma padrão (ex: 'de-DE', 'pt-BR')
  googlevisibility: false # 'true' para permitir visibilidade ao Google (via robots.txt)
  enableAlphaFunctionality: false # habilita funcionalidades em teste
  showUpdate: false # mostra quando uma nova atualização está disponível
  showUpdateOnlyAdmin: false # apenas admins veem atualizações
  customHTMLFiles: false # permite que arquivos em /customFiles/templates sobrescrevam os existentes
  tessdataDir: /usr/share/tessdata # caminho para os arquivos Tessdata
  enableAnalytics: null # 'true' para habilitar analytics, 'false' para desabilitar
  enableUrlToPDF: false # INTERNAL, não use externamente por questões de segurança
  disableSanitize: false # 'true' para desabilitar a sanitização de HTML (risco de injeção)
  datasource:
    enableCustomDatabase: false # apenas para usuários Enterprise
    customDatabaseUrl: '' # ex: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
    type: postgresql # 'h2' ou 'postgresql'
    hostName: localhost
    port: 5432
    name: postgres
  customPaths:
    pipeline:
      watchedFoldersDir: '' # padrão: /pipeline/watchedFolders
      finishedFoldersDir: '' # padrão: /pipeline/finishedFolders
    operations:
      weasyprint: '' # padrão: /opt/venv/bin/weasyprint
      unoconvert: '' # padrão: /opt/venv/bin/unoconvert
  fileUploadLimit: '' # ex: "25MB", "100KB". Vazio para sem limite.
```

#### `ui`
```yaml
ui:
  appName: '' # nome visível da aplicação
  homeDescription: '' # descrição curta na página inicial
  appNameNavbar: '' # nome na barra de navegação
  languages: [] # ex: ["de_DE", "pl_PL"]. Vazio para todos os idiomas.
```

#### `endpoints`
```yaml
endpoints:
  toRemove: [] # lista de endpoints a serem desabilitados, ex: ['img-to-pdf', 'remove-pages']
  groupsToRemove: [] # lista de grupos a serem desabilitados, ex: ['LibreOffice']
```

#### `metrics`
```yaml
metrics:
  enabled: true # 'true' para habilitar endpoints de Info (`/api/*`)
```

#### `processExecutor`
```yaml
processExecutor:
  sessionLimit:
    libreOfficeSessionLimit: 1
    pdfToHtmlSessionLimit: 1
    qpdfSessionLimit: 4
    tesseractSessionLimit: 1
    pythonOpenCvSessionLimit: 8
    weasyPrintSessionLimit: 16
    installAppSessionLimit: 1
    calibreSessionLimit: 1
  timeoutMinutes:
    libreOfficetimeoutMinutes: 30
    pdfToHtmltimeoutMinutes: 20
    pythonOpenCvtimeoutMinutes: 30
    weasyPrinttimeoutMinutes: 30
    installApptimeoutMinutes: 60
    calibretimeoutMinutes: 30
    tesseractTimeoutMinutes: 30
```

### Configurações Adicionais (`custom_settings.yml`)

Para usuários familiarizados com Java e `application.properties` do Spring, é possível adicionar configurações personalizadas no arquivo `/configs/custom_settings.yml`. Para mais informações sobre configurações como SSL ou modo DEBUG, [consulte a documentação](https://docs.stirlingpdf.com/).

---

## Notas Adicionais

### Desabilitando Endpoints
Use as variáveis `ENDPOINTS_TOREMOVE` e `ENDPOINTS_GROUPSTOREMOVE` com listas separadas por vírgula.
- `ENDPOINTS_TOREMOVE=img-to-pdf,remove-pages` desabilita os endpoints `img-to-pdf` e `remove-pages`.
- `ENDPOINTS_GROUPSTOREMOVE=LibreOffice` desabilita todas as funcionalidades que usam LibreOffice.

### Personalizando a Interface
Para customizar arquivos estáticos como o logo da aplicação, coloque os arquivos no diretório `/customFiles/static/`. Por exemplo, para alterar o logo, coloque seu arquivo em `/customFiles/static/favicon.svg`. Isso pode ser usado para alterar qualquer imagem, ícone, CSS, fonte, JS, etc.

### Parâmetros Apenas de Ambiente
- `SYSTEM_ROOTURIPATH`: Define a URI raiz da aplicação (ex: `/pdf-app`).
- `SYSTEM_CONNECTIONTIMEOUTMINUTES`: Define o tempo limite de conexão.
- `DISABLE_ADDITIONAL_FEATURES`: `false` para baixar o JAR de segurança (necessário para autenticação e recursos pro).
- `DISABLE_PIXEL`: `true` ou `false` para desabilitar o pixel de rastreamento.
- `LANGS`: Define bibliotecas de fontes customizadas a serem instaladas para conversão de documentos.

---

## Exemplos de Variáveis de Ambiente

#### Local (Linux/macOS)
```bash
export UI_APPNAME="Stirling PDF"
```

#### Local (Windows CMD)
```cmd
set UI_APPNAME="Stirling PDF"
```

#### Local (Windows PowerShell)
```powershell
$env:UI_APPNAME="Stirling PDF"
```

#### Docker Run
```bash
docker run ... \
  -e "UI_APPNAME=Stirling PDF" \
  -e "UI_HOMEDESCRIPTION=Sua loja local completa para todas as suas necessidades de PDF." \
  -e "UI_APPNAVBARNAME=Stirling PDF"
```

#### Docker Compose
Adicione ao `environment` no seu `docker-compose.yml`:
```yaml
services:
  stirling-pdf:
    # ...
    environment:
      - UI_APPNAME=Stirling PDF
      - UI_HOMEDESCRIPTION=Sua loja local completa para todas as suas necessidades de PDF.
      - UI_APPNAVBARNAME=Stirling PDF
```
