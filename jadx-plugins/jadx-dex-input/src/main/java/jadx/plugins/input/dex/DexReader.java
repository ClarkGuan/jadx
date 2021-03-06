package jadx.plugins.input.dex;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.IClassData;
import jadx.plugins.input.dex.sections.DexClassData;
import jadx.plugins.input.dex.sections.DexHeader;
import jadx.plugins.input.dex.sections.SectionReader;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;

public class DexReader implements Closeable {

	private final Path path;
	private final ByteBuffer buf;
	private final DexHeader header;

	public DexReader(Path path) throws IOException {
		this.path = path;
		this.buf = ByteBuffer.wrap(Files.readAllBytes(path));
		this.header = new DexHeader(new SectionReader(this, 0));
	}

	public String getDexVersion() {
		return this.header.getVersion();
	}

	public void visitClasses(Consumer<IClassData> consumer) {
		int count = header.getClassDefsSize();
		if (count == 0) {
			return;
		}
		int classDefsOff = header.getClassDefsOff();
		SectionReader in = new SectionReader(this, classDefsOff);
		AnnotationsParser annotationsParser = new AnnotationsParser(in.copy(), in.copy());
		DexClassData classData = new DexClassData(in, annotationsParser);
		for (int i = 0; i < count; i++) {
			consumer.accept(classData);
			in.shiftOffset(DexClassData.SIZE);
		}
	}

	public ByteBuffer getBuf() {
		return buf;
	}

	public DexHeader getHeader() {
		return header;
	}

	public Path getPath() {
		return path;
	}

	public String getFullPath() {
		StringBuilder sb = new StringBuilder();
		FileSystem fileSystem = path.getFileSystem();
		if (fileSystem.getClass().getName().contains("Zip")) {
			sb.append(fileSystem.toString()).append(':');
		}
		sb.append(path.toAbsolutePath());
		return sb.toString();
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public String toString() {
		return getFullPath();
	}
}
