DESTDIR = outd
SOURCEDIR = src/fcup

sourcefiles = $(wildcard $(SOURCEDIR)/*.java)
classfiles = $(patsubst $(SOURCEDIR)/%.java, $(DESTDIR)/fcup/%.class, $(sourcefiles) )

all: $(DESTDIR)/fcup $(classfiles)

$(classfiles): $(sourcefiles)
	javac -d $(DESTDIR) $(sourcefiles)

$(DESTDIR)/fcup:
	mkdir -p $(DESTDIR)/fcup

clean:
	rm -rf $(DESTDIR)

test:
	./test.sh

stop:
	killall rmiregistry
