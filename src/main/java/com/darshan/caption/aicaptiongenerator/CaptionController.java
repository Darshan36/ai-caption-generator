package com.darshan.caption.aicaptiongenerator;

import com.darshan.caption.aicaptiongenerator.model.Caption;
import com.darshan.caption.aicaptiongenerator.model.User;
import com.darshan.caption.aicaptiongenerator.repository.CaptionRepository;
import com.darshan.caption.aicaptiongenerator.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
// import java.util.stream.Collectors; // This is no longer needed

@RestController
@RequestMapping("/api/caption")
public class CaptionController {

    private static final Logger logger = LoggerFactory.getLogger(CaptionController.class);

    private final GeminiService geminiService;
    private final CaptionRepository captionRepository;
    private final UserRepository userRepository;

    public CaptionController(GeminiService geminiService, CaptionRepository captionRepository, UserRepository userRepository) {
        this.geminiService = geminiService;
        this.captionRepository = captionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateCaption(
            // Make the image parameter optional to handle the error gracefully
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "style", defaultValue = "Standard") String style, 
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // --- NEW: Server-side check for the image file ---
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select an image file to upload.");
        }
        // --- END NEW CHECK ---

        if (userDetails == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Pass the style to the service
            String captionText = geminiService.generateCaption(image, style);

            Caption caption = new Caption();
            caption.setGeneratedText(captionText);
            caption.setOriginalImageName(image.getOriginalFilename());
            caption.setUser(user);
            captionRepository.save(caption);

            return ResponseEntity.ok(captionText);
        } catch (Exception e) {
            logger.error("Error generating caption", e);
            return ResponseEntity.status(500).body("Error generating caption: " + e.getMessage());
        }
    }

    // --- REVERTED THIS METHOD ---
    @GetMapping("/history")
    public ResponseEntity<List<Caption>> getCaptionHistory(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        // Directly find and return the original Caption objects
        List<Caption> captions = captionRepository.findByUser_UsernameOrderByCreatedAtDesc(userDetails.getUsername());
        return ResponseEntity.ok(captions);
    }
}

