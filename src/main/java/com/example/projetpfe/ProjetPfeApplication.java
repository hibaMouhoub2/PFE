package com.example.projetpfe;

import com.example.projetpfe.dto.UserDto;
import com.example.projetpfe.service.Impl.BrancheService;
import com.example.projetpfe.service.Impl.RegionService;
import com.example.projetpfe.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProjetPfeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetPfeApplication.class, args);
	}
	@Bean
	CommandLineRunner init(UserService userService, RegionService regionService, BrancheService brancheService) {
		return args -> {
			// Initialiser les régions
			regionService.initializeRegions();




		};
	}

}
