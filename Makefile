DESTDIR = outd
SOURCEDIR = src/fcup
LIB = lib/jline-3.5.1.jar

sourcefiles = $(wildcard $(SOURCEDIR)/*.java)
classfiles = $(patsubst $(SOURCEDIR)/%.java, $(DESTDIR)/fcup/%.class, $(sourcefiles) )

all: $(DESTDIR)/fcup $(classfiles)

$(classfiles): $(sourcefiles)
	javac -d $(DESTDIR) $(sourcefiles) -cp $(LIB)

$(DESTDIR)/fcup:
	mkdir -p $(DESTDIR)/fcup

clean:
	rm -rf $(DESTDIR)

prepare:
	./prepare.sh

test:
	./test.sh

stop:
	killall rmiregistry
