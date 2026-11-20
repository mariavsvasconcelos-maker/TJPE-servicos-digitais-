package br.org.cesar.tjpe.validador.service.validator;

import org.springframework.stereotype.Component;

/**
 * Validador estrito dos algoritmos oficiais da Receita Federal do Brasil
 * para CPF (11 dígitos) e CNPJ (14 dígitos), com verificação de dígitos verificadores (Módulo 11).
 */
@Component
public class CpfCnpjValidator {

    public boolean isValid(String documento) {
        if (documento == null) return false;
        String limpo = documento.replaceAll("\\D", "");
        if (limpo.length() == 11) {
            return isCpfValid(limpo);
        } else if (limpo.length() == 14) {
            return isCnpjValid(limpo);
        }
        return false;
    }

    public boolean isCpfValid(String cpf) {
        if (cpf == null) return false;
        String limpo = cpf.replaceAll("\\D", "");
        if (limpo.length() != 11) return false;

        // Rejeita sequências repetidas conhecidas (ex: 111.111.111-11)
        if (limpo.matches("(\\d)\\1{10}")) return false;

        try {
            int soma1 = 0;
            for (int i = 0; i < 9; i++) {
                soma1 += (limpo.charAt(i) - '0') * (10 - i);
            }
            int r1 = 11 - (soma1 % 11);
            int dv1 = (r1 > 9) ? 0 : r1;
            if (dv1 != (limpo.charAt(9) - '0')) return false;

            int soma2 = 0;
            for (int i = 0; i < 10; i++) {
                soma2 += (limpo.charAt(i) - '0') * (11 - i);
            }
            int r2 = 11 - (soma2 % 11);
            int dv2 = (r2 > 9) ? 0 : r2;
            return dv2 == (limpo.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isCnpjValid(String cnpj) {
        if (cnpj == null) return false;
        String limpo = cnpj.replaceAll("\\D", "");
        if (limpo.length() != 14) return false;

        if (limpo.matches("(\\d)\\1{13}")) return false;

        try {
            int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma1 = 0;
            for (int i = 0; i < 12; i++) {
                soma1 += (limpo.charAt(i) - '0') * peso1[i];
            }
            int r1 = soma1 % 11;
            int dv1 = (r1 < 2) ? 0 : (11 - r1);
            if (dv1 != (limpo.charAt(12) - '0')) return false;

            int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma2 = 0;
            for (int i = 0; i < 13; i++) {
                soma2 += (limpo.charAt(i) - '0') * peso2[i];
            }
            int r2 = soma2 % 11;
            int dv2 = (r2 < 2) ? 0 : (11 - r2);
            return dv2 == (limpo.charAt(13) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public String formatar(String documento) {
        if (documento == null) return null;
        String limpo = documento.replaceAll("\\D", "");
        if (limpo.length() == 11) {
            return limpo.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else if (limpo.length() == 14) {
            return limpo.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return documento;
    }
}
