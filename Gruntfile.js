var pkgjson = require("./package.json")

var config = {
	pkg: pkgjson,
	app: 'bower_components',
	dist: 'src/main/webapp/static',
	src: 'src/main/webapp/assets'
}

module.exports = function(grunt) {

	// Configuration
	grunt.initConfig({
		config: config,
		pkg: config.pkg,
		bower: grunt.file.readJSON("./.bowerrc"),
		copy: {
			dist: {
				files: [{
					expand: true,
					cwd: '<%= config.app %>/konva',
					src: 'konva.min.js',
					dest: '<%= config.dist %>/js'
				},{
					expand: true,
					cwd: '<%= config.app %>/beemuse/dist',
					src: 'beemuse.min.css',
					dest: '<%= config.dist %>/css'
				},{
					expand: true,
					cwd: '<%= config.app %>/babylon-grid/dist',
					src: 'jquery.babylongrid.min.js',
					dest: '<%= config.dist %>/js'
				},{
					expand: true,
					cwd: '<%= config.app %>/babylon-grid/dist/css',
					src: 'babylongrid-default.css',
					dest: '<%= config.dist %>/css'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-pagewalkthrough/dist',
					src: 'jquery.pagewalkthrough.min.js',
					dest: '<%= config.dist %>/js'
				},{
					expand: true,
					cwd: '<%= config.app %>/parallax.js',
					src: 'parallax.min.js',
					dest: '<%= config.dist %>/js'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-pagewalkthrough/dist/css',
					src: 'jquery.pagewalkthrough.min.css',
					dest: '<%= config.dist %>/css'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-pagewalkthrough/dist/css/images',
					src: '**',
					dest: '<%= config.dist %>/css/images'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-pagewalkthrough/dist/css/font',
					src: '**',
					dest: '<%= config.dist %>/css/font'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-ui/',
					src: 'jquery-ui.min.js',
					dest: '<%= config.dist %>/js'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-ui/themes/base',
					src: 'jquery-ui.min.css',
					dest: '<%= config.dist %>/css'
				},{
					expand: true,
					cwd: '<%= config.app %>/jquery-ui/themes/base/images',
					src: '**',
					dest: '<%= config.dist %>/css/images'
				},{
					expand: true,
					cwd: '<%= config.app %>/ionicons/fonts',
					src: '**',
					dest: '<%= config.dist %>/fonts'
				},{
					expand: true,
					cwd: '<%= config.app %>/ionicons/css',
					src: 'ionicons.min.css',
					dest: '<%= config.dist %>/css'
				}]
			}
		},
		/*concat: {
			dist: {
				src: [
					'<%= config.app %>/jquery-ui/ui/minified/core.js',
					'<%= config.app %>/jquery-ui/ui/minified/widget.js',
					'<%= config.app %>/jquery-ui/ui/minified/position.js',
					'<%= config.app %>/jquery-ui/ui/minified/data.js',
					'<%= config.app %>/jquery-ui/ui/minified/keycode.js',
					'<%= config.app %>/jquery-ui/ui/minified/scroll-parent.js',
					'<%= config.app %>/jquery-ui/ui/minified/unique-id.js',
					'<%= config.app %>/jquery-ui/ui/widgets/sortable.js',
					'<%= config.app %>/jquery-ui/ui/widgets/autocomplete.js',
					'<%= config.app %>/jquery-ui/ui/widgets/menu.js',
					'<%= config.app %>/jquery-ui/ui/widgets/mouse.js'
				],
				dest: '<%= config.dist %>/js/jquery-ui.js'
			}
		},*/
		uglify: {
			dist: {
				options: {
					mangle: false,
					preserveComments: function(node, comment) {
						return /Copyright/.test(comment.value);
					}
				},
				files: [{
					expand: true,
					cwd: '<%= config.src %>/js',
					src: ['*.js', '!*.min.js'],
					dest: '<%= config.dist %>/js',
					rename: function(dst, src) {
						// To keep the source js files and make new files as `*.min.js`:
						return dst + '/' + src.replace('.js', '.min.js');
						// Or to override to src:
						//return src;
					}
				}]
			}
		},
		cssmin: {
			dist: {
				files: [{
					expand: true,
					cwd: '<%= config.src %>/css',
					src: ['*.css', '!*.min.css'],
					dest: '<%= config.dist %>/css',
					ext: '.min.css'
				}]
			}
		},
		coffee: {
			dist: {
				options: {
					bare: true
				},
				files: {
					'<%= config.dist %>/js/main.js': '<%= config.src %>/coffee/main.coffee'
				}
			}
		}
	});

	grunt.loadNpmTasks('grunt-contrib-copy');
	//grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-contrib-uglify');
	grunt.loadNpmTasks('grunt-contrib-cssmin');
	grunt.loadNpmTasks('grunt-contrib-coffee');

    grunt.registerTask('build', ['copy', 'uglify', 'cssmin', 'coffee']);
};