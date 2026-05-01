# AtvidadeDM

Aplicativo Android de **gerenciamento de viagens** desenvolvido em **Jetpack Compose**.

## Funcionalidades implementadas

- Login com validação local baseada nos usuários cadastrados
- Cadastro de usuário com validação completa
- Recuperação de senha por e-mail cadastrado
- Menu principal com `DrawerMenu`
- Tela de **Nova Viagem** com:
  - destino
  - tipo (lazer ou negócios)
  - data de início
  - data de fim
  - orçamento
  - validação obrigatória
  - seleção de datas com `DatePicker`
- Tela de **Minhas Viagens** com:
  - listagem apenas do usuário logado
  - ícone visual para lazer e negócios
  - edição por **long click**
  - exclusão por **swipe**
- Tela **Sobre**
- Persistência local com **Room**
- **Migration** do banco da versão 1 para a versão 2
- Estado gerenciado com **ViewModel**
- Navegação entre telas

## Estrutura em camadas

- `data/` → repositórios
- `data/local/` → Room, entidades e DAOs
- `navigation/` → rotas e navegação principal
- `ui/screen/` → telas Compose
- `ui/viewmodel/` → ViewModels e factories

## Banco de dados

Banco local: `travel_app.db`

Entidades principais:
- `UserEntity`
- `TripEntity`

## Como executar

No Windows PowerShell:

```powershell
Set-Location "C:\Users\abel.fonseca\StudioProjects\AtvidadeDM"
.\gradlew.bat assembleDebug
```

## Observação sobre login

Para conseguir entrar no aplicativo, primeiro faça o cadastro em **Novo Usuário**. Depois utilize o mesmo e-mail e senha na tela de login.

