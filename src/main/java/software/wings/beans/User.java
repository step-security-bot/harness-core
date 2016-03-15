package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import javax.security.auth.Subject;
import java.security.Principal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 *  User bean class.
 *
 *
 * @author Rishi
 *
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "users", noClassnameStored = true)
public class User extends Base implements Principal {
  private String name;
  @Indexed(unique = true) private String email;
  @JsonIgnore private String passwordHash;
  private long lastLogin;

  @Transient private String token;

  @Override
  public String getName() {
    return name;
  }
  @Override
  public boolean implies(Subject subject) {
    return false;
  }

  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  public String getPasswordHash() {
    return passwordHash;
  }
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }
  public void setLastLogin(long lastLogin) {
    this.lastLogin = lastLogin;
  }
  public long getLastLogin() {
    return lastLogin;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getToken() {
    return token;
  }
  public void setToken(String token) {
    this.token = token;
  }
}
