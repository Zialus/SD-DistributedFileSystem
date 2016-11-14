DESTDIR = outd
SOURCEDIR = src/fcup

sourcefiles = $(wildcard $(SOURCEDIR)/*.java)
classfiles = $(wildcard $(DESTDIR)/fcup/*.class)

all: doit

doit: $(sourcefiles)
	mkdir -p $(DESTDIR)/fcup
	javac -d $(DESTDIR) $(sourcefiles)

clean:
	rm -rf $(DESTDIR)
