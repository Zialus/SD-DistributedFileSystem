DESTDIR = outd
SOURCEDIR = src/main/java/fcup

JLINE_VERSION = 4.3.1
JLINE = lib/jline-$(JLINE_VERSION).jar
LOMBOK_VERSION = 1.18.46
LOMBOK = lib/lombok-$(LOMBOK_VERSION).jar
ANNOT_VERSION = 3.0.1
ANNOT = lib/annotations-$(ANNOT_VERSION).jar

CP = $(JLINE):$(LOMBOK):$(ANNOT)

sourcefiles = $(wildcard $(SOURCEDIR)/*.java)
classfiles = $(patsubst $(SOURCEDIR)/%.java, $(DESTDIR)/fcup/%.class, $(sourcefiles) )

all: $(DESTDIR)/fcup $(classfiles)

$(classfiles): $(sourcefiles)
	javac -d $(DESTDIR) $(sourcefiles) -cp "$(CP)" -processorpath "$(LOMBOK)"

$(DESTDIR)/fcup:
	mkdir -p $(DESTDIR)/fcup

clean:
	rm -rf $(DESTDIR)

download:
	mkdir -p lib
	wget -O $(JLINE) https://repo1.maven.org/maven2/org/jline/jline/$(JLINE_VERSION)/jline-$(JLINE_VERSION).jar
	wget -O $(LOMBOK) https://repo1.maven.org/maven2/org/projectlombok/lombok/$(LOMBOK_VERSION)/lombok-$(LOMBOK_VERSION).jar
	wget -O $(ANNOT) https://repo1.maven.org/maven2/com/google/code/findbugs/annotations/$(ANNOT_VERSION)/annotations-$(ANNOT_VERSION).jar

test:
	./test.sh

stop:
	killall java
