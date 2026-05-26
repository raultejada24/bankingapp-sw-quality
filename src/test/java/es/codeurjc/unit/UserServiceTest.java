package es.codeurjc.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.codeurjc.model.User;
import es.codeurjc.repository.UserRepository;
import es.codeurjc.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService createService() {
        return new UserService(userRepository);
    }

    @Test
    @DisplayName("1. isMinor_ReturnsTrueForMinor: Devuelve true para un usuario menor de edad")
    void isMinor_ReturnsTrueForMinorTest() {
        User user = new User();
        user.setId(1L);
        user.setBirthDate(LocalDate.now().minusYears(17));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertTrue(createService().isMinor(1L));
    }

    @Test
    @DisplayName("2. isMinor_ReturnsFalseForAdult: Devuelve false para un usuario mayor de edad")
    void isMinor_ReturnsFalseForAdultTest() {
        User user = new User();
        user.setId(2L);
        user.setBirthDate(LocalDate.now().minusYears(20));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertFalse(createService().isMinor(2L));
    }

    @Test
    @DisplayName("3. isMinor_ReturnsFalseWhenBirthDateMissing: Devuelve false si la fecha de nacimiento es nula")
    void isMinor_ReturnsFalseWhenBirthDateMissingTest() {
        User user = new User();
        user.setId(3L);
        user.setBirthDate(null);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertFalse(createService().isMinor(3L));
    }
}
