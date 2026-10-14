package dev.linkshelf.link;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import dev.linkshelf.link.validation.MaxCodePoints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkController {

	private final LinkService linkService;

	public LinkController(LinkService linkService) {
		this.linkService = linkService;
	}

	@PostMapping
	public ResponseEntity<LinkResponse> save(@Valid @RequestBody SaveLinkRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(linkService.save(request));
	}

	/**
	 * Constraints on parameters are checked by Spring's built-in method validation. The class is deliberately not
	 * {@code @Validated}: that would raise ConstraintViolationException and change the error shape (research R4).
	 */
	@GetMapping
	public LinkPageResponse list(
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "0 이상이어야 합니다") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "1 이상 100 이하여야 합니다") @Max(value = 100,
					message = "1 이상 100 이하여야 합니다") int size,
			@RequestParam(required = false) @Pattern(regexp = "(?U).*\\S.*",
					message = "공백만 있을 수 없습니다") @MaxCodePoints(30) String tag) {
		return linkService.list(page, size, tag);
	}

}
