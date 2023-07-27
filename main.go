package main

import (
	"fmt"
	"os"

	"github.com/rsvihladremio/dremio-stress/cmd"
)

func main() {
	args, err := cmd.ParseArgs()
	if err != nil {
		fmt.Println(args)
		fmt.Printf("error parsing args: %+v\n", err)
		os.Exit(1)
	}

	if err := cmd.Execute(args); err != nil {
		fmt.Println(args)
		fmt.Printf("error executing dremio-stress: %+v", err)
		os.Exit(1)
	}
}
