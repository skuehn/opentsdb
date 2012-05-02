# Copyright (C) 2011-2012  The OpenTSDB Authors.
#
# This library is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published
# by the Free Software Foundation, either version 2.1 of the License, or
# (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this library.  If not, see <http://www.gnu.org/licenses/>.

CLOUDTRACE_VERSION := 1.3.5-incubating
CLOUDTRACE := third_party/cloudtrace/cloudtrace-$(CLOUDTRACE_VERSION).jar
CLOUDTRACE_BASE_URL := https://repository.apache.org/content/groups/public/org/apache/accumulo/cloudtrace/$(CLOUDTRACE_VERSION)

$(CLOUDTRACE): $(CLOUDTRACE).md5
	set dummy "$(CLOUDTRACE_BASE_URL)" "$(CLOUDTRACE)"; shift; $(FETCH_DEPENDENCY)

THIRD_PARTY += $(CLOUDTRACE)
