package garage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import javax.annotation.PostConstruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import garage.vehicles.boundaries.DetailedVehicleBoundary;
import garage.vehicles.boundaries.VehicleBoundary;
import garage.vehicles.boundaries.VehicleTypeBoundary;
import garage.vehicles.misc.VehicleTypes;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class GetVehicleTests {
  private int port;
  private WebClient webClient;
  
  @LocalServerPort
  public void setPort(int port) {
    this.port = port;
  }
  
  @PostConstruct
  public void init() {
    String baseUrl = "http://localhost:" + port + "/api/v1/vehicles";
    webClient = WebClient.create(baseUrl);
  }
  
  @AfterEach
  public void tearDown() {
    webClient.delete()
            .uri("/admin/delete")
            .retrieve()
            .bodyToMono(Void.class)
            .log()
            .block();  
  }
  
  @Test
  public void getValidVehicleTest() throws Exception {
    var licenseNumber = "00-000-00";
    // given
    var vehicleBoundary = new VehicleBoundary(new VehicleTypeBoundary("car", "Electric"), "Hyundai", licenseNumber, 20, 50);
    // and
    webClient.post()
            .bodyValue(vehicleBoundary)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .log()
            .block();
    
    // when
    var response = webClient.get()
            .uri("/{licenseNumber}", licenseNumber)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .log()
            .block();
    
    // then
    assertThat(response).isNotNull();
    assertThat(response.vehicleType().getType()).isEqualTo(VehicleTypes.Car.toString().toLowerCase());
    assertThat(response.licenseNumber()).isEqualTo(licenseNumber);
  }
  
  @Test
  public void notFoundVehicleTest() throws Exception {
    var licenseNumber = "00-010-00";
    // given
    var vehicleBoundary = new VehicleBoundary(new VehicleTypeBoundary("car", "Electric"), "Hyundai", "00-000-00", 20, 50);
    // and
    webClient.post()
            .bodyValue(vehicleBoundary)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .log()
            .block();
    
    // when
    // then
    assertThrows(WebClientResponseException.NotFound.class, () -> webClient.get()
            .uri("/{licenseNumber}", licenseNumber)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .log()
            .block());
  }
  
  @Test
  public void getVehicleInvalidLicenseNumberTest() throws Exception {
    var invaliLicenseNumber = "00-00-00";
    // given
    var vehicleBoundary = new VehicleBoundary(new VehicleTypeBoundary("car", "Electric"), "Hyundai", "00-000-00", 20, 50);
    // and
    webClient.post()
            .bodyValue(vehicleBoundary)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .log()
            .block();
    
    // when
    // then
    assertThrows(WebClientResponseException.BadRequest.class, () -> webClient.get()
            .uri("/{licenseNumber}", invaliLicenseNumber)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .block());
  }
}
