import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiClient {
  // Altere para o IP do seu Gateway (ex: 10.0.2.2 para Emulador Android, ou IP local da máquina)
  static const String baseUrl = 'http://10.0.2.2:8080'; 

  // Método genérico para requisições GET com Token JWT
  Future<http.Response> get(String endpoint) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    String? token = prefs.getString('jwt_token');

    final response = await http.get(
      Uri.parse('$baseUrl$endpoint'),
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );
    return response;
  }

  // Método genérico para requisições POST
  Future<http.Response> post(String endpoint, Map<String, dynamic> body) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    String? token = prefs.getString('jwt_token');

    final response = await http.post(
      Uri.parse('$baseUrl$endpoint'),
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );
    return response;
  }
}