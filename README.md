# AtvidadeDM

Aplicativo Android de **gerenciamento de viagens** desenvolvido em **Jetpack Compose**.

## ✨ Destaques da Versão Atual

- ✅ **Mapa com OpenStreetMap Gratuito** (sem necessidade de API key!)
- ✅ **Geração de Roteiros com Gemini 2.5 Flash**
- ✅ **Campo de Comentários** para viagens
- ✅ **100% Gratuito** - Sem custos de API
- ✅ **Banco de Dados Escalável** com migrations automáticas

## 🎯 Funcionalidades Implementadas

### Autenticação
- Login com validação local baseada nos usuários cadastrados
- Cadastro de usuário com validação completa
- Recuperação de senha por e-mail cadastrado

### Gerenciamento de Viagens
- Cadastro de viagem com:
  - Destino
  - Tipo (lazer ou negócios)
  - Data de início
  - Data de fim
  - Orçamento
  - **Comentários adicionais** ← Novo!
  - Validação obrigatória
  - Seletor de datas com `DatePicker`

- Listagem de viagens com:
  - Ícones visuais (lazer/negócios)
  - Edição por **long click**
  - Exclusão por **swipe**
  - Filtro por usuário logado

### Mapa Interativo
- **OpenStreetMap Mapnik** (Gratuito, sem API key necessária)
- Marcador de localização atual da viagem
- Controles de zoom e pan
- Atribuição de créditos automática (ODbL)

### Roteiros Turísticos com IA
- **Gemini 2.5 Flash** integrado
- Formulário com:
  - Destino
  - Período
  - Interesses
  - **Comentários** ← Novo!
  - Estilo de viagem
  - Orçamento
- Validação: requer **interesses OU comentários**
- Exibição formatada do roteiro gerado

### Galeria de Fotos
- Visualizar fotos da viagem
- Adicionar fotos (câmera ou galeria)
- Persistência local

### Interface
- Design moderno com **Material Design 3**
- Navegação intuitiva com **Bottom Bar**
- Menu principal com `DrawerMenu`
- Tela **Sobre**

## 🏗️ Arquitetura

### Estrutura em Camadas (MVVM)
```
data/
├── local/          → Room, entidades, DAOs
├── remote/
│   └── gemini/    → Retrofit, cliente da IA
└── TripRepository → Lógica de negócio

ui/
├── screen/        → Telas Compose
├── viewmodel/     → ViewModels, State Management
└── navigation/    → Rotas e Navegação
```

### Camadas Implementadas
- **Data Layer**: Repository, API Client, Local Database
- **Domain Layer**: Use cases, Business Logic
- **UI Layer**: ViewModels, Screens, Compose UI
- **Navigation**: Rotas estruturadas

## 🗄️ Banco de Dados

**Banco local**: `travel_app.db`

**Entidades principais**:
- `UserEntity` - Usuários do app
- `TripEntity` - Viagens (v5: com campo `comments`)
- `TripPhotoEntity` - Fotos das viagens

**Migrations**:
- v1→v2: Criação de tabela `trips`
- v2→v3: Adição de `total_spent`
- v3→v4: Criação de tabela `trip_photos`
- v4→v5: Adição de coluna `comments` em trips ← Novo!

## 🔧 Como Executar

### 1. No Windows PowerShell
```powershell
Set-Location "C:\Users\abel.fonseca\StudioProjects\AtvidadeDM"
.\gradlew.bat assembleDebug
```

### 2. Abrir no Android Studio
- File → Open → Selecionar a pasta do projeto

### 3. Configurar API (Gemini)

Crie arquivo `local.secrets.properties` na **raiz do projeto**:

```properties
GEMINI_API_KEY=seu_token_aqui
```

### 4. Nenhuma outra configuração necessária!

O mapa usa **OpenStreetMap** (gratuito, sem necessidade de chave).

## 🗺️ Mapa: OpenStreetMap vs Google Maps

| Aspecto | OpenStreetMap | Google Maps |
|---------|---|---|
| **Custo** | ✅ Gratuito | Pago (após quota) |
| **Chave API** | ✅ Não necessária | Necessária |
| **Limite** | ✅ Ilimitado | 1.000/dia grátis |
| **Open Source** | ✅ Sim | Não |
| **Implementação** | Osmdroid 6.1.18 | Google Play Services |

## 📦 Dependências Principais

```gradle
// Mapa
osmdroid = 6.1.18  // OpenStreetMap (Gratuito)

// API
retrofit = 2.11.0
gson = 2.11.0

// Banco de Dados
room = 2.7.2

// UI
compose = 2024.09.00
material3

// Android
lifecycle = 2.10.0
playserviceslocation = 21.3.0
```

## 🔐 Segurança

### Chaves de API
- `GEMINI_API_KEY` salva em `local.secrets.properties`
- Arquivo Git-ignored (não vai para repositório)
- Seguro apenas na máquina local

### Permissões
- `INTERNET` - Para OpenStreetMap e Gemini
- `ACCESS_FINE_LOCATION` - Localização precisa
- `ACCESS_COARSE_LOCATION` - Localização aproximada

## 📝 Como Usar

### 1. Criar Viagem
```
Home → "Nova Viagem"
├─ Preencher: Destino, Tipo, Datas, Orçamento
├─ Adicionar: Comentários (opcional)
└─ Salvar
```

### 2. Ver Mapa
```
Home (com viagem ativa)
├─ Mapa OpenStreetMap aparece
├─ Zoom e pan com dedos
└─ Marcador com nome da viagem
```

### 3. Gerar Roteiro
```
Viagem → Aba "Roteiro"
├─ Dados carregam automaticamente
├─ Adicionar: Interesses ou Comentários (obrigatório)
└─ Clicar: "Gerar roteiro com IA"
```

### 4. Adicionar Fotos
```
Viagem → Aba "Fotos"
├─ Ver galeria
├─ Adicionar foto (câmera ou galeria)
└─ Salvar vinculado à viagem
```

## 📖 Documentação Adicional

- **`GUIA_RAPIDO.md`** - Guia rápido de uso (LEIA PRIMEIRO!)
- **`RESUMO_ALTERACOES.md`** - Resumo técnico completo
- **`MAPS_OPENSTREETMAP_SETUP.md`** - Documentação do mapa
- **`CHECKLIST_VALIDACAO.md`** - Lista de verificação funcional

## ℹ️ Observações de Login

Para entrar no aplicativo:
1. Clique em **"Novo Usuário"** para cadastrar-se
2. Use o mesmo e-mail e senha no **login**
3. Pronto! Você pode criar e gerenciar suas viagens

## 🚀 Status do Projeto

✅ **Funcionalidades**: 100% implementadas  
✅ **Build**: Sem erros (BUILD SUCCESSFUL)  
✅ **Segurança**: Chaves API seguras  
✅ **Performance**: Otimizado  
✅ **Pronto para Produção!**

## 📞 Suporte

Se encontrar algum problema:
1. Verifique a conexão de internet
2. Limpe o cache: `./gradlew clean`
3. Reinstale o app
4. Consulte `RESUMO_ALTERACOES.md`

---

**Última atualização**: 23/06/2026  
**Versão**: 1.0 (Produção)  
**Status**: ✅ Pronto
