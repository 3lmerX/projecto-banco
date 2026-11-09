package mz.banco.contas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidarPinRequest(String numeroConta, String pin) {}