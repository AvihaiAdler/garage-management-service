package garage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import garage.vehicles.boundaries.DetailedVehicleBoundary;
import garage.vehicles.boundaries.VehicleBoundary;
import garage.vehicles.boundaries.VehicleTypeBoundary;
import garage.vehicles.misc.EnergySourceTypes;
import garage.vehicles.misc.VehicleTypes;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class GetAllVehiclesTests {
  private int port;
  private WebClient webClient;
  private List<VehicleBoundary> vehicles;
  
  @LocalServerPort
  public void setPort(int port) {
    this.port = port;
  }
  
  @PostConstruct
  public void init() {
    String baseUrl = "http://localhost:" + port + "/api/v1/vehicles";
    webClient = WebClient.create(baseUrl);
    
    vehicles = List.of(
            new VehicleBoundary(new VehicleTypeBoundary("truck", null), "Man", "00-000-00", 15, 95),
            new VehicleBoundary(new VehicleTypeBoundary("car", "electric"), "Hyundai", "00-010-00", 10, 45),
            new VehicleBoundary(new VehicleTypeBoundary("car", "regular"), "Mazda", "00-020-00", 55, 45),
            new VehicleBoundary(new VehicleTypeBoundary("car", "regular"), "Hyundai", "00-030-00", 55, 45),
            new VehicleBoundary(new VehicleTypeBoundary("motorcycle", "electric"), "Honda", "00-040-00", 89, 25),
            new VehicleBoundary(new VehicleTypeBoundary("motorcycle", "electric"), "Suzuki", "00-050-00", 22, 30),
            new VehicleBoundary(new VehicleTypeBoundary("motorcycle", "regular"), "Honda", "00-060-00", 47, 22));
  }
  
  @BeforeEach
  public void populateDb() {
    vehicles.forEach(vehicle -> webClient.post()
            .bodyValue(vehicle)
            .retrieve()
            .bodyToMono(DetailedVehicleBoundary.class)
            .block());
  }
  
  @AfterEach
  public void tearDown() {
    webClient.delete()
            .uri("/admin/delete")
            .retrieve()
            .bodyToMono(Void.class)
            .block();
  }
  
  @Test
  public void getAllVehiclesNoFilterDefaultSortTest() throws Exception {
    // given    
    var sortBy = "licenseNumber";
    
    // when
    var response = webClient.get()
            .uri("?sortBy={sortBy}", sortBy)
            .retrieve()
            .bodyToFlux(DetailedVehicleBoundary.class)
            .log()
            .collectList()
            .block();
    
    // then
    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(vehicles.size());
    assertThat(response.get(0).licenseNumber()).isEqualTo(vehicles.get(0).licenseNumber());
    assertThat(response.get(response.size()-1).licenseNumber()).isEqualTo(vehicles.get(vehicles.size()-1).licenseNumber());
  }
  
  @Test
  public void getCarsTest() throws Exception {
    // given
    var numOfCars = vehicles.stream()
            .filter(vehicle -> vehicle.vehicleType().getType().equalsIgnoreCase(VehicleTypes.Car.toString()))
            .toList()
            .size();
    
    var filterType = "byVehicleType";
    var filterValue = VehicleTypes.Car.toString();
    // when
    var response = webClient.get()
            .uri("?filterType={filterType}&filterValue={filterValue}", filterType, filterValue)
            .retrieve()
            .bodyToFlux(DetailedVehicleBoundary.class)
            .log()
            .collectList()
            .block();
    
    // then
    assertThat(response).isNotEmpty();
    assertThat(response.size()).isEqualTo(numOfCars);
  }
  
  @Test
  public void getRegularVehiclesTest() throws Exception {
    // given
    var numOfRegularVehicles = vehicles.stream()
            .map(VehicleBoundary::vehicleType)
            .map(VehicleTypeBoundary::getEnergySource)
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .filter(Predicate.isEqual(EnergySourceTypes.Regular.toString().toLowerCase()))
            .toList()
            .size();
    
    var filterType = "byEnergyType";
    var filterValue = EnergySourceTypes.Regular.toString().toLowerCase();
    // when
    var response = webClient.get()
            .uri("?filterType={filterType}&filterValue={filterValue}", filterType, filterValue)
            .retrieve()
            .bodyToFlux(DetailedVehicleBoundary.class)
            .log()
            .collectList()
            .block();
    
    // then
    assertThat(response).isNotEmpty();
    assertThat(response.size()).isEqualTo(numOfRegularVehicles);
  }
  
  @Test
  public void getVehiclesWithInvalidFilterTest() throws Exception {
    // given
    var invalidFilterType = "byTirePressure";
    
    // when
    // then
    assertThrows(WebClientResponseException.BadRequest.class, () -> webClient.get()
            .uri("?filterType={filterType}", invalidFilterType)
            .retrieve()
            .bodyToFlux(DetailedVehicleBoundary.class)
            .log()
            .collectList()
            .block());
  }
  
  @Test
  public void getRegularCarsWithPaginationTest() throws Exception {
    // given 
    var numOfRegularVehicles = vehicles.stream()
            .map(VehicleBoundary::vehicleType)
            .map(VehicleTypeBoundary::getEnergySource)
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .filter(Predicate.isEqual(EnergySourceTypes.Regular.toString().toLowerCase()))
            .toList()
            .size();
    
    var size = 2;
    var page = 1;
    var expected = numOfRegularVehicles%size;
    
    var filterType = "byEnergyType";
    var filterValue = EnergySourceTypes.Regular.toString().toLowerCase();
    
    // when
    var response = webClient.get()
            .uri("?filterType={filterType}&filterValue={filterValue}&page={page}&size={size}", filterType, filterValue, page, size)
            .retrieve()
            .bodyToFlux(DetailedVehicleBoundary.class)
            .log()
            .collectList()
            .block();
    
    // then
    assertThat(response).isNotEmpty();
    assertThat(response.size()).isEqualTo(expected);
  }
}
