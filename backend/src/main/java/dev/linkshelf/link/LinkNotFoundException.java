package dev.linkshelf.link;

public class LinkNotFoundException extends RuntimeException {

	private final long id;

	public LinkNotFoundException(long id) {
		super("Link not found: " + id);
		this.id = id;
	}

	public long getId() {
		return id;
	}

}
