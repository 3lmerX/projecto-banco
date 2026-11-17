class BankService {
  final ApiClient _apiClient = ApiClient();

  // 1. Autenticação (Login)
  Future<bool> login(String username, String password) async {
    try {
      final response = await _apiClient.post('/auth/login', {
        'username': username,
        'password': password,
      });

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        String token = data['token']; // Ajuste conforme o AuthDtos$TokenResponse do seu projeto

        // Guardar token localmente
        SharedPreferences prefs = await SharedPreferences.getInstance();
        await prefs.setString('jwt_token', token);
        return true;
      }
      return false;
    } catch (e) {
      print('Erro no login: $e');
      return false;
    }
  }

  // 2. Obter dados da Conta
  Future?> getDetalhesConta(String numeroConta) async {
    try {
      final response = await _apiClient.get('/contas/$numeroConta');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
    } catch (e) {
      print('Erro ao buscar conta: $e');
    }
    return null;
  }

  // 3. Realizar Transferência (Integrando com o módulo de transações/contas)
  Future<bool> realizarTransferencia(String contaOrigem, String contaDestino, double valor) async {
    try {
      final response = await _apiClient.post('/transaccoes/transferir', {
        'contaOrigem': contaOrigem,
        'contaDestino': contaDestino,
        'valor': valor,
      });

      return response.statusCode == 200 || response.statusCode == 201;
    } catch (e) {
      print('Erro na transferência: $e');
      return false;
    }
  }
}