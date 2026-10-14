package dev.linkshelf.link;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A saved link (data-model.md, approved 2026-10-14). Column lengths are UTF-16 units sized at twice the code-point
 * limit, because H2 counts VARCHAR length in UTF-16 units (research R1).
 */
@Entity
@Table(name = "link",
		uniqueConstraints = @UniqueConstraint(name = Link.NORMALIZED_URL_CONSTRAINT, columnNames = "normalized_url"))
public class Link {

	/** Named so a violation can be told apart from other integrity errors (research R3). */
	static final String NORMALIZED_URL_CONSTRAINT = "uk_link_normalized_url";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "normalized_url", length = 4096, nullable = false)
	private String normalizedUrl;

	@Column(name = "memo", length = 1000)
	private String memo;

	@ElementCollection
	@CollectionTable(name = "link_tag", joinColumns = @JoinColumn(name = "link_id"))
	@OrderColumn(name = "position")
	@Column(name = "name", length = 60, nullable = false)
	private List<String> tags = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Link() {
	}

	public Link(String normalizedUrl, String memo, List<String> tags, Instant createdAt) {
		this.normalizedUrl = normalizedUrl;
		this.memo = memo;
		this.tags = new ArrayList<>(tags);
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getNormalizedUrl() {
		return normalizedUrl;
	}

	public String getMemo() {
		return memo;
	}

	public List<String> getTags() {
		return List.copyOf(tags);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
