DESTDIR = outd
SOURCEDIR = src/main/java/fcup
LIB = lib/jline-3.10.0.jar

sourcefiles = $(wildcard $(SOURCEDIR)/*.java)
classfiles = $(patsubst $(SOURCEDIR)/%.java, $(DESTDIR)/fcup/%.class, $(sourcefiles) )

all: $(DESTDIR)/fcup $(classfiles)

$(classfiles): $(sourcefiles)
	javac -d $(DESTDIR) $(sourcefiles) -cp $(LIB)

$(DESTDIR)/fcup:
	mkdir -p $(DESTDIR)/fcup

clean:
	rm -rf $(DESTDIR)

download:
	mkdir -p lib
	wget https://repo1.maven.org/maven2/org/jline/jline/3.10.0/jline-3.10.0.jar -O $(LIB)

test:
	./test.sh

stop:
	killall java
