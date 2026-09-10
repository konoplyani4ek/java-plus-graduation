package ewm.main.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 254)
    @Email
    @NotBlank
    @NotNull
    @Size(min = 6, max = 254)
    private String email;

    @Column(name = "name", nullable = false, length = 250)
    @NotBlank
    @Size(min = 2, max = 250)
    @NotNull
    private String name;
}